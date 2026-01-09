package com.example.smartpos.service

import com.example.smartpos.model.EmvCardData
import com.example.smartpos.model.TcpMessage
import com.example.smartpos.network.TcpConnectionService
import com.example.smartpos.network.TcpConnectionState
import com.example.smartpos.network.TransactionResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TransactionTcpManager(
    private val tcpService: TcpConnectionService
) {

    fun listen(
        scope: CoroutineScope,
        onTransaction: (TransactionResponse) -> Unit
    ) {
        scope.launch {
            tcpService.connectionState.collect { state ->
                if (state is TcpConnectionState.DataReceived) {
                    onTransaction(state.response)
                }
            }
        }
    }

    suspend fun sendStarted(
        terminalId: String,
        amount: String,
        transactionId: String?,
        pcPosId: String?
    ) {
        val msg = TcpMessage.createTransactionStarted(
            trmId = terminalId,
            amount = amount,
            transactionId = transactionId,
            pcPosId = pcPosId
        )
        tcpService.sendTransactionMessage(msg)
    }

    suspend fun sendCompleted(
        terminalId: String,
        transactionId: String,
        emvData: EmvCardData
    ) {
        val msg = TcpMessage.createTransactionCompleted(
            trmId = terminalId,
            transactionId = transactionId,
            emvCardData = emvData
        )
        tcpService.sendTransactionMessage(msg)
    }
}
