package com.example.smartpos.data.repository

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object TcpRepository {
    private val _messages = MutableSharedFlow<String>(replay = 0)
    val messages = _messages.asSharedFlow()

    private val _connectionStatus = MutableSharedFlow<Boolean>()
    val connectionStatus = _connectionStatus.asSharedFlow()

    suspend fun emitMessage(message: String) {
        _messages.emit(message)
    }

    suspend fun updateStatus(isConnected: Boolean) {
        _connectionStatus.emit(isConnected)
    }
}