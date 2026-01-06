package com.example.smartpos.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.coroutines.coroutineContext

class TcpClientManager {
    private var socket: Socket? = null

    suspend fun startConnection(
        host: String,
        port: Int,
        onDataReceived: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                socket = Socket()
                socket?.connect(InetSocketAddress(host, port), 5000)
                socket?.keepAlive = true

                val reader = socket?.getInputStream()?.bufferedReader()
                while (coroutineContext.isActive && socket?.isConnected == true && !socket!!.isClosed) {
                    val line = reader?.readLine() ?: break
                    onDataReceived(line)
                }
            } catch (e: Exception) {
                onError(e)
            } finally {
                close()
            }
        }
    }

    fun close() {
        try {
            socket?.close()
            socket = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}