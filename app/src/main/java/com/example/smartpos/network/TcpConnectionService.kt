package com.example.smartpos.network

import android.util.Log
import com.example.smartpos.model.TcpMessage
import com.example.smartpos.model.TerminalConfig
import com.example.smartpos.utils.AtomicFlag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.Locale
import kotlin.math.min

data class TransactionResponse(
    val transactionType: String,
    val amount: String? = null,
    val status: String? = null,
    val message: String? = null,
    val transactionId: String? = null,
    val tipAmt: Double? = null,
    val currCd: String? = null,
    val terminalId: String? = null,
    val pcPosId: String? = null
)

sealed class TcpConnectionState {
    object Idle : TcpConnectionState()
    object Connecting : TcpConnectionState()
    object Connected : TcpConnectionState()
    data class DataReceived(val response: TransactionResponse) : TcpConnectionState()
    data class Error(val message: String) : TcpConnectionState()
}

class TcpConnectionService {
    companion object {
        private const val TAG = "TcpConnectionService"
        private const val MAX_BACKOFF_MS = 60000L  // Max 60 seconds between retries
        private const val INITIAL_BACKOFF_MS = 5000L  // Initial 5 seconds
    }

    // Thread-safe state management
    private val mutex = Mutex()
    private val isRunning = AtomicFlag(false)
    private val isBankConnected = AtomicFlag(false)
    
    // Socket resources (protected by mutex)
    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null
    
    // Bank Connector separate socket (protected by mutex)
    private var bankSocket: Socket? = null
    private var bankWriter: PrintWriter? = null
    private var bankReader: BufferedReader? = null
    
    // Terminal configuration
    private val terminalConfig = TerminalConfig()

    private val _connectionState = MutableStateFlow<TcpConnectionState>(TcpConnectionState.Idle)
    val connectionState: StateFlow<TcpConnectionState> = _connectionState.asStateFlow()

    /**
     * Kết nối tới TCP endpoint với exponential backoff retry
     * Thread-safe implementation with proper resource management
     */
    suspend fun connect() = withContext(Dispatchers.IO) {
        var retryCount = 0
        var currentBackoff = INITIAL_BACKOFF_MS

        while (!isRunning.value) {
            // Check for coroutine cancellation
            ensureActive()
            
            try {
                _connectionState.value = TcpConnectionState.Connecting
                if (retryCount > 0) {
                    Log.d(TAG, "Retry attempt #$retryCount - Connecting to ${TcpConfig.CURRENT_HOST}:${TcpConfig.CURRENT_PORT}...")
                } else {
                    Log.d(TAG, "Connecting to ${TcpConfig.CURRENT_HOST}:${TcpConfig.CURRENT_PORT}...")
                }

                // Thread-safe socket creation
                mutex.withLock {
                    // Cleanup any existing connection first
                    closeMainSocketSafely()
                    
                    // Create socket with connect timeout
                    socket = Socket().apply {
                        connect(
                            InetSocketAddress(TcpConfig.CURRENT_HOST, TcpConfig.CURRENT_PORT),
                            TcpConfig.CONNECT_TIMEOUT_MS
                        )
                        soTimeout = TcpConfig.SOCKET_TIMEOUT_MS
                        keepAlive = TcpConfig.KEEP_ALIVE_ENABLED
                        tcpNoDelay = true
                    }

                    // Initialize reader and writer
                    reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                    writer = PrintWriter(socket!!.getOutputStream(), true)
                }

                _connectionState.value = TcpConnectionState.Connected
                Log.d(TAG, "Connected successfully!")

                isRunning.set(true)
                currentBackoff = INITIAL_BACKOFF_MS  // Reset backoff on success

                // Gửi initial message (msgType = 0)
                sendInitialMessage()
                
                // Kết nối tới bank connector (non-blocking)
                connectToBankConnector()

                // Bắt đầu lắng nghe dữ liệu
                listenForData()
                
                // If listenForData returns, connection was lost
                if (!isRunning.value) {
                    retryCount++
                    kotlinx.coroutines.delay(currentBackoff)
                    currentBackoff = min(currentBackoff * 2, MAX_BACKOFF_MS)  // Exponential backoff
                }

            } catch (e: SocketTimeoutException) {
                retryCount++
                Log.e(TAG, "Timeout when connecting (attempt #$retryCount)", e)
                _connectionState.value = TcpConnectionState.Error("Timeout - Retrying in ${currentBackoff/1000}s...")
                closeMainSocketSafely()
                kotlinx.coroutines.delay(currentBackoff)
                currentBackoff = min(currentBackoff * 2, MAX_BACKOFF_MS)
            } catch (e: Exception) {
                retryCount++
                Log.e(TAG, "Failed to connect (attempt #$retryCount): ${e.message}", e)
                _connectionState.value = TcpConnectionState.Error("Failed - Retrying in ${currentBackoff/1000}s...")
                closeMainSocketSafely()
                kotlinx.coroutines.delay(currentBackoff)
                currentBackoff = min(currentBackoff * 2, MAX_BACKOFF_MS)
            }
        }
    }
    
    /**
     * Close main socket resources safely without throwing exceptions
     */
    private fun closeMainSocketSafely() {
        try { reader?.close() } catch (e: Exception) { Log.w(TAG, "Error closing reader: ${e.message}") }
        try { writer?.close() } catch (e: Exception) { Log.w(TAG, "Error closing writer: ${e.message}") }
        try { socket?.close() } catch (e: Exception) { Log.w(TAG, "Error closing socket: ${e.message}") }
        reader = null
        writer = null
        socket = null
        isRunning.set(false)
    }

    /**
     * Gửi initial message khi kết nối (msgType = 0)
     */
    private suspend fun sendInitialMessage() = withContext(Dispatchers.IO) {
        try {
            val initMessage = TcpMessage.createInitMessage(terminalConfig.trmId)
            val jsonString = initMessage.toJson()
            
            mutex.withLock {
                writer?.println(jsonString)
                writer?.flush()
            }
            Log.d(TAG, "Send initial msg: $jsonString")
        } catch (e: Exception) {
            Log.e(TAG, "Failed when sending msg", e)
        }
    }

    /**
     * Lắng nghe dữ liệu từ server
     * Thread-safe with cancellation support
     */
    private suspend fun listenForData() = withContext(Dispatchers.IO) {
        try {
            while (isRunning.value && socket?.isConnected == true) {
                // Check for cancellation
                ensureActive()
                
                // Đọc dữ liệu từ server
                val line = reader?.readLine()
                
                if (line != null) {
                    Log.d(TAG, "Received data: $line")
                    
                    // Parse JSON
                    try {
                        val response = parseJsonResponse(line)
                        _connectionState.value = TcpConnectionState.DataReceived(response)
                        Log.d(TAG, "Parse successfully: TransactionType=${response.transactionType}")

                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi khi parse JSON", e)
                        _connectionState.value = TcpConnectionState.Error("Invalid data: ${e.message}")
                    }
                } else {
                    // Connection closed by server
                    Log.d(TAG, "Server closed connection")
                    _connectionState.value = TcpConnectionState.Error("Server closed connection")
                    isRunning.set(false)
                    break
                }
            }
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Timeout while reading data", e)
            _connectionState.value = TcpConnectionState.Error("Timeout while waiting for data")
            isRunning.set(false)
        } catch (e: Exception) {
            Log.e(TAG, "Failed when reading data", e)
            _connectionState.value = TcpConnectionState.Error("Failed when receiving data: ${e.message}")
            isRunning.set(false)
        }
    }

    /**
     * Parse JSON response từ server
     * Thread-safe, uses optDouble/optString for null safety
     */
    private fun parseJsonResponse(jsonString: String): TransactionResponse {
        val jsonObject = JSONObject(jsonString)
        
        // Parse amount - support both "amount" and "TotTrAmt" fields
        // Use optDouble with default to prevent JSONException
        val amountStr = when {
            jsonObject.has("TotTrAmt") -> {
                val totAmt = jsonObject.optDouble("TotTrAmt", 0.0)
                String.format(Locale.US, "%.0f", totAmt)  // Use US locale for period separator
            }
            jsonObject.has("amount") -> jsonObject.optString("amount", null)
            else -> null
        }
        
        // Parse transaction ID - support both "TransactionId" and "transactionId" and "ID"
        val txnId = when {
            jsonObject.has("TransactionId") -> jsonObject.optString("TransactionId")
            jsonObject.has("transactionId") -> jsonObject.optString("transactionId")
            jsonObject.has("ID") -> jsonObject.optString("ID")
            else -> null
        }
        
        // Parse TipAmt
        val tipAmt = when {
            jsonObject.has("TipAmt") -> jsonObject.optDouble("TipAmt", 0.0)
            jsonObject.has("tipAmt") -> jsonObject.optDouble("tipAmt", 0.0)
            else -> null
        }
        
        // Parse CurrCd (Currency Code)
        val currCd = when {
            jsonObject.has("CurrCd") -> jsonObject.optString("CurrCd", "VND")
            jsonObject.has("currCd") -> jsonObject.optString("currCd", "VND")
            jsonObject.has("currency") -> jsonObject.optString("currency", "VND")
            else -> null
        }
        
        // Parse TerminalId
        val terminalId = when {
            jsonObject.has("TerminalId") -> jsonObject.optString("TerminalId")
            jsonObject.has("terminalId") -> jsonObject.optString("terminalId")
            else -> null
        }
        
        // Parse PcPosId
        val pcPosId = when {
            jsonObject.has("PcPosId") -> jsonObject.optString("PcPosId")
            jsonObject.has("pcPosId") -> jsonObject.optString("pcPosId")
            else -> null
        }
        
        return TransactionResponse(
            transactionType = jsonObject.optString("TransactionType", "UNKNOWN"),
            amount = amountStr,
            status = jsonObject.optString("Status"),
            message = jsonObject.optString("ErrorDetail"),
            transactionId = txnId,
            tipAmt = tipAmt,
            currCd = currCd,
            terminalId = terminalId,
            pcPosId = pcPosId
        )
    }

    /**
     * Gửi dữ liệu tới server (nếu cần)
     */
    suspend fun sendData(data: String) = withContext(Dispatchers.IO) {
        try {
            mutex.withLock {
                writer?.println(data)
                writer?.flush()
            }
            Log.d(TAG, "Đã gửi dữ liệu: $data")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gửi dữ liệu", e)
            _connectionState.value = TcpConnectionState.Error("Lỗi khi gửi dữ liệu: ${e.message}")
        }
    }

    /**
     * Gửi transaction message (thread-safe)
     */
    suspend fun sendTransactionMessage(message: TcpMessage) = withContext(Dispatchers.IO) {
        try {
            val jsonString = message.toJson()
            mutex.withLock {
                writer?.println(jsonString)
                writer?.flush()
            }
            Log.d(TAG, "Transaction sent: $jsonString")
        } catch (e: Exception) {
            Log.e(TAG, "Failed when sending transaction", e)
            _connectionState.value = TcpConnectionState.Error("Lỗi khi gửi message: ${e.message}")
        }
    }
    
    /**
     * Close bank connector resources safely
     */
    private fun closeBankConnectorSafely() {
        try { bankReader?.close() } catch (e: Exception) { Log.w(TAG, "Error closing bank reader: ${e.message}") }
        try { bankWriter?.close() } catch (e: Exception) { Log.w(TAG, "Error closing bank writer: ${e.message}") }
        try { bankSocket?.close() } catch (e: Exception) { Log.w(TAG, "Error closing bank socket: ${e.message}") }
        bankReader = null
        bankWriter = null
        bankSocket = null
        isBankConnected.set(false)
    }
    
    /**
     * Kết nối tới bank connector server
     * Thread-safe với proper resource cleanup
     */
    private suspend fun connectToBankConnector() = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                Log.d(TAG, "Connecting to bank connector at ${TcpConfig.BANK_CONNECTOR_HOST}:${TcpConfig.BANK_CONNECTOR_PORT}...")
                
                // Cleanup existing connection first
                closeBankConnectorSafely()
                
                bankSocket = Socket().apply {
                    connect(
                        InetSocketAddress(TcpConfig.BANK_CONNECTOR_HOST, TcpConfig.BANK_CONNECTOR_PORT),
                        TcpConfig.CONNECT_TIMEOUT_MS
                    )
                    soTimeout = TcpConfig.SOCKET_TIMEOUT_MS
                    keepAlive = TcpConfig.KEEP_ALIVE_ENABLED
                    tcpNoDelay = true
                }
                
                bankWriter = PrintWriter(bankSocket!!.getOutputStream(), true)
                bankReader = BufferedReader(InputStreamReader(bankSocket!!.getInputStream()))
                isBankConnected.set(true)
                
                Log.d(TAG, "Bank connector connected successfully!")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to bank connector: ${e.message}", e)
                closeBankConnectorSafely()
            }
        }
    }
    
    /**
     * Send message to bank connector (thread-safe with auto-reconnect)
     */
    suspend fun sendToBankConnector(message: TcpMessage) = withContext(Dispatchers.IO) {
        // Try to reconnect if not connected (outside of mutex to avoid deadlock)
        if (!isBankConnected.value || bankSocket?.isConnected != true) {
            Log.w(TAG, "Bank connector not connected, attempting to reconnect...")
            connectToBankConnector()
        }
        
        mutex.withLock {
            try {
                if (isBankConnected.value && bankWriter != null) {
                    val jsonString = message.toJson()
                    bankWriter?.println(jsonString)
                    bankWriter?.flush()
                    Log.d(TAG, "Sent to bank connector: $jsonString")
                } else {
                    Log.e(TAG, "Unable to send to bank connector - connection not available")
                    _connectionState.value = TcpConnectionState.Error("Bank connector unavailable")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending to bank connector", e)
                isBankConnected.set(false)
                _connectionState.value = TcpConnectionState.Error("Failed to send to bank: ${e.message}")
            }
        }
    }

    fun getTerminalId(): String {
        return terminalConfig.trmId
    }

    /**
     * Get terminal config
     */
    fun getTerminalConfig(): TerminalConfig {
        return terminalConfig
    }

    /**
     * Gửi keep-alive message để giữ connection (thread-safe)
     */
    suspend fun sendKeepAlive() = withContext(Dispatchers.IO) {
        try {
            mutex.withLock {
                writer?.println("{\"type\":\"keepalive\"}")
                writer?.flush()
            }
            Log.d(TAG, "Đã gửi keep-alive")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gửi keep-alive", e)
        }
    }

    /**
     * Đóng tất cả connections (thread-safe)
     */
    fun disconnect() {
        isRunning.set(false)
        closeMainSocketSafely()
        closeBankConnectorSafely()
        Log.d(TAG, "Đã đóng tất cả connections")
    }

    /**
     * Reset state về Idle
     */
    fun resetState() {
        _connectionState.value = TcpConnectionState.Idle
    }
    
    /**
     * Check if connected to main server
     */
    fun isConnected(): Boolean = isRunning.value && socket?.isConnected == true
    
    /**
     * Check if connected to bank connector
     */
    fun isBankConnectorConnected(): Boolean = isBankConnected.value && bankSocket?.isConnected == true
}
