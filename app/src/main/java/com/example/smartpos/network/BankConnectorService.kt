package com.example.smartpos.network

import android.util.Log
import com.example.smartpos.model.TcpMessage
import com.example.smartpos.utils.AtomicFlag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

/**
 * Sealed class for Bank Connector connection states
 */
sealed class BankConnectorState {
    object Disconnected : BankConnectorState()
    object Connecting : BankConnectorState()
    object Connected : BankConnectorState()
    data class Error(val message: String) : BankConnectorState()
}

/**
 * Separate service for bank connector communication
 * Uses its own socket connection on port 8888
 * Thread-safe implementation with proper resource management
 */
class BankConnectorService {
    companion object {
        private const val TAG = "BankConnectorService"
        private const val MAX_RECONNECT_ATTEMPTS = 3
        private const val RECONNECT_DELAY_MS = 2000L
    }
    
    // Thread-safe state management
    private val mutex = Mutex()
    private val isConnected = AtomicFlag(false)
    
    // Socket resources
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    
    // Connection state
    private val _state = MutableStateFlow<BankConnectorState>(BankConnectorState.Disconnected)
    val state: StateFlow<BankConnectorState> = _state.asStateFlow()
    
    /**
     * Connect to bank connector server
     * Thread-safe and handles resource cleanup on failure
     */
    suspend fun connect(
        host: String = TcpConfig.BANK_CONNECTOR_HOST,
        port: Int = TcpConfig.BANK_CONNECTOR_PORT
    ): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            // Already connected
            if (isConnected.value && socket?.isConnected == true) {
                Log.d(TAG, "Already connected to bank connector")
                return@withContext Result.success(Unit)
            }
            
            _state.value = BankConnectorState.Connecting
            Log.d(TAG, "Connecting to bank connector at $host:$port...")
            
            try {
                // Cleanup any existing connection first
                closeResourcesSafely()
                
                // Create new socket with timeout
                socket = Socket().apply {
                    connect(java.net.InetSocketAddress(host, port), TcpConfig.CONNECT_TIMEOUT_MS)
                    soTimeout = TcpConfig.SOCKET_TIMEOUT_MS
                    keepAlive = TcpConfig.KEEP_ALIVE_ENABLED
                    tcpNoDelay = true
                }
                
                // Initialize streams
                writer = PrintWriter(socket!!.getOutputStream(), true)
                reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                
                isConnected.set(true)
                _state.value = BankConnectorState.Connected
                Log.d(TAG, "Bank connector connected successfully!")
                
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to bank connector: ${e.message}", e)
                closeResourcesSafely()
                isConnected.set(false)
                _state.value = BankConnectorState.Error("Connection failed: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * Send message to bank connector with auto-reconnect
     * Returns Result to indicate success/failure
     */
    suspend fun send(message: TcpMessage): Result<Unit> = withContext(Dispatchers.IO) {
        // Try to reconnect if not connected
        if (!isConnected.value || socket?.isConnected != true) {
            Log.w(TAG, "Bank connector not connected, attempting to reconnect...")
            var attempt = 0
            while (attempt < MAX_RECONNECT_ATTEMPTS) {
                attempt++
                val connectResult = connect()
                if (connectResult.isSuccess) break
                
                if (attempt < MAX_RECONNECT_ATTEMPTS) {
                    Log.d(TAG, "Reconnect attempt $attempt failed, retrying in ${RECONNECT_DELAY_MS}ms...")
                    kotlinx.coroutines.delay(RECONNECT_DELAY_MS)
                }
            }
        }
        
        mutex.withLock {
            if (!isConnected.value || writer == null) {
                val error = "Bank connector unavailable after reconnect attempts"
                Log.e(TAG, error)
                _state.value = BankConnectorState.Error(error)
                return@withContext Result.failure(IllegalStateException(error))
            }
            
            try {
                val jsonString = message.toJson()
                writer?.println(jsonString)
                writer?.flush()
                Log.d(TAG, "Sent to bank connector: $jsonString")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending to bank connector", e)
                isConnected.set(false)
                _state.value = BankConnectorState.Error("Send failed: ${e.message}")
                closeResourcesSafely()
                Result.failure(e)
            }
        }
    }
    
    /**
     * Read response from bank connector
     * Returns null if no response or error
     */
    suspend fun readResponse(): String? = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!isConnected.value || reader == null) {
                return@withContext null
            }
            
            try {
                reader?.readLine()
            } catch (e: Exception) {
                Log.e(TAG, "Error reading from bank connector: ${e.message}", e)
                null
            }
        }
    }
    
    /**
     * Disconnect from bank connector
     * Thread-safe resource cleanup
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        mutex.withLock {
            closeResourcesSafely()
            isConnected.set(false)
            _state.value = BankConnectorState.Disconnected
            Log.d(TAG, "Bank connector disconnected")
        }
    }
    
    /**
     * Check if connected
     */
    fun isConnected(): Boolean = isConnected.value && socket?.isConnected == true
    
    /**
     * Close resources safely without throwing exceptions
     */
    private fun closeResourcesSafely() {
        try { reader?.close() } catch (e: Exception) { Log.w(TAG, "Error closing reader: ${e.message}") }
        try { writer?.close() } catch (e: Exception) { Log.w(TAG, "Error closing writer: ${e.message}") }
        try { socket?.close() } catch (e: Exception) { Log.w(TAG, "Error closing socket: ${e.message}") }
        reader = null
        writer = null
        socket = null
    }
}
