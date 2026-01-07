package com.example.smartpos.network

import android.util.Log
import com.example.smartpos.model.TcpMessage
import com.example.smartpos.model.TerminalConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.net.SocketTimeoutException

data class TransactionResponse(
    val transactionType: String,
    val amount: String? = null,
    val status: String? = null,
    val message: String? = null,
    val transactionId: String? = null
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
    }

    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null
    private var isRunning = false
    
    // Terminal configuration
    private val terminalConfig = TerminalConfig()

    private val _connectionState = MutableStateFlow<TcpConnectionState>(TcpConnectionState.Idle)
    val connectionState: StateFlow<TcpConnectionState> = _connectionState.asStateFlow()

    /**
     * Kết nối tới TCP endpoint và giữ sống connection với auto-retry
     */
    suspend fun connect() = withContext(Dispatchers.IO) {
        var retryCount = 0
        val maxRetries = Int.MAX_VALUE // Retry vô hạn
        val retryDelayMs = 5000L // 5 giây giữa mỗi lần retry

        while (retryCount < maxRetries && !isRunning) {
            try {
                _connectionState.value = TcpConnectionState.Connecting
                if (retryCount > 0) {
                    Log.d(TAG, "Retry attempt #$retryCount - Connecting to ${TcpConfig.CURRENT_HOST}:${TcpConfig.CURRENT_PORT}...")
                } else {
                    Log.d(TAG, "Connecting to ${TcpConfig.CURRENT_HOST}:${TcpConfig.CURRENT_PORT}...")
                }

                // Tạo socket connection
                socket = Socket(TcpConfig.CURRENT_HOST, TcpConfig.CURRENT_PORT).apply {
                    soTimeout = TcpConfig.SOCKET_TIMEOUT_MS
                    keepAlive = TcpConfig.KEEP_ALIVE_ENABLED
                    tcpNoDelay = true
                }

                // Khởi tạo reader và writer
                reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                writer = PrintWriter(socket!!.getOutputStream(), true)

                _connectionState.value = TcpConnectionState.Connected
                Log.d(TAG, "Connected successfully!")

                isRunning = true

                // Gửi initial message (msgType = 0)
                sendInitialMessage()

                // Bắt đầu lắng nghe dữ liệu
                listenForData()
                
                // Nếu connection bị đứt, retry lại
                if (!isRunning) {
                    retryCount++
                    kotlinx.coroutines.delay(retryDelayMs)
                }

            } catch (e: SocketTimeoutException) {
                retryCount++
                Log.e(TAG, "Timeout when connecting (attempt #$retryCount)", e)
                _connectionState.value = TcpConnectionState.Error("Timeout - Retrying...")
                disconnect()
                kotlinx.coroutines.delay(retryDelayMs)
            } catch (e: Exception) {
                retryCount++
                Log.e(TAG, "Failed to connect (attempt #$retryCount): ${e.message}", e)
                _connectionState.value = TcpConnectionState.Error("Failed - Retrying...")
                disconnect()
                kotlinx.coroutines.delay(retryDelayMs)
            }
        }
    }

    /**
     * Gửi initial message khi kết nối (msgType = 0)
     */
    private suspend fun sendInitialMessage() = withContext(Dispatchers.IO) {
        try {
            val initMessage = TcpMessage.createInitMessage(terminalConfig.trmId)
            val jsonString = initMessage.toJson()
            
            writer?.println(jsonString)
            writer?.flush()
            Log.d(TAG, "Send initial msg: $jsonString")
        } catch (e: Exception) {
            Log.e(TAG, "Failed when sending msg", e)
        }
    }

    /**
     * Lắng nghe dữ liệu từ server
     */
    private suspend fun listenForData() = withContext(Dispatchers.IO) {
        try {
            while (isRunning && socket?.isConnected == true) {
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
                    disconnect()
                    break
                }
            }
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Timeout while reading data", e)
            _connectionState.value = TcpConnectionState.Error("Timeout while waiting for data")
            disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Failed when reading data", e)
            _connectionState.value = TcpConnectionState.Error("Failed when receiving data: ${e.message}")
            disconnect()
        }
    }

    /**
     * Parse JSON response từ server
     */
    private fun parseJsonResponse(jsonString: String): TransactionResponse {
        val jsonObject = JSONObject(jsonString)
        
        return TransactionResponse(
            transactionType = jsonObject.optString("TransactionType", "UNKNOWN"),
            amount = jsonObject.optString("amount"),
            status = jsonObject.optString("status"),
            message = jsonObject.optString("message"),
            transactionId = jsonObject.optString("transactionId")
        )
    }

    /**
     * Gửi dữ liệu tới server (nếu cần)
     */
    suspend fun sendData(data: String) = withContext(Dispatchers.IO) {
        try {
            writer?.println(data)
            writer?.flush()
            Log.d(TAG, "Đã gửi dữ liệu: $data")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gửi dữ liệu", e)
            _connectionState.value = TcpConnectionState.Error("Lỗi khi gửi dữ liệu: ${e.message}")
        }
    }

    /**
     * Gửi transaction message
     */
    suspend fun sendTransactionMessage(message: TcpMessage) = withContext(Dispatchers.IO) {
        try {
            val jsonString = message.toJson()
            writer?.println(jsonString)
            writer?.flush()
            Log.d(TAG, "Đã gửi transaction message: $jsonString")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gửi transaction message", e)
            _connectionState.value = TcpConnectionState.Error("Lỗi khi gửi message: ${e.message}")
        }
    }    
    /**
     * Gửi transaction tới bank connector server
     * TODO: Có thể cần tạo socket riêng tới bank connector
     */
    suspend fun sendToBankConnector(message: TcpMessage) = withContext(Dispatchers.IO) {
        try {
            // TODO: Implement separate connection to bank connector if needed
            // For now, using same connection
            val jsonString = message.toJson()
            writer?.println(jsonString)
            writer?.flush()
            Log.d(TAG, "Sent to bank connector: $jsonString")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending to bank connector", e)
            _connectionState.value = TcpConnectionState.Error("Failed to send to bank: ${e.message}")
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
     * Gửi keep-alive message để giữ connection
     */
    suspend fun sendKeepAlive() = withContext(Dispatchers.IO) {
        try {
            writer?.println("{\"type\":\"keepalive\"}")
            writer?.flush()
            Log.d(TAG, "Đã gửi keep-alive")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gửi keep-alive", e)
        }
    }

    /**
     * Đóng connection
     */
    fun disconnect() {
        try {
            isRunning = false
            reader?.close()
            writer?.close()
            socket?.close()
            Log.d(TAG, "Đã đóng connection")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi đóng connection", e)
        } finally {
            socket = null
            reader = null
            writer = null
        }
    }

    /**
     * Reset state về Idle
     */
    fun resetState() {
        _connectionState.value = TcpConnectionState.Idle
    }
}
