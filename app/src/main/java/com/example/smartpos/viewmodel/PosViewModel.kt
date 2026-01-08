package com.example.smartpos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import androidx.navigation.NavController
import com.example.smartpos.model.CardData
import com.example.smartpos.model.EmvCardData
import com.example.smartpos.model.TcpMessage
import com.example.smartpos.network.TcpConnectionService
import com.example.smartpos.network.TcpConnectionState
import com.example.smartpos.network.TransactionResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

enum class TransactionType {
    SALE, QR, VOID, REFUND
}

data class Transaction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: TransactionType,
    val name: String,
    val amount: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isVoided: Boolean = false
)

class PosViewModel : ViewModel() {
    companion object {
        private const val TAG = "PosViewModel"
    }
    
    // Navigation controller reference
    private var navController: NavController? = null

    private val _amount = MutableStateFlow("0")
    val amount = _amount.asStateFlow()

    private val _selectedTip = MutableStateFlow(0)
    val selectedTip = _selectedTip.asStateFlow()

    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState = _paymentState.asStateFlow()

    private val _transactionHistory = MutableStateFlow<List<Transaction>>(emptyList())
    val transactionHistory = _transactionHistory.asStateFlow()

    private val tcpService = TcpConnectionService()
    val tcpConnectionState: StateFlow<TcpConnectionState> = tcpService.connectionState

    private val _emvCardData = MutableStateFlow<EmvCardData?>(null)
    val emvCardData = _emvCardData.asStateFlow()

    private val _nfcData = MutableStateFlow<String?>(null)
    val nfcData = _nfcData.asStateFlow()

    private val _cardData = MutableStateFlow<CardData?>(null)
    val cardData = _cardData.asStateFlow()

    private val _isWaitingForNfc = MutableStateFlow(false)
    val isWaitingForNfc = _isWaitingForNfc.asStateFlow()
    
    // Store current transaction ID from server
    private val _currentTransactionId = MutableStateFlow<String?>(null)
    val currentTransactionId = _currentTransactionId.asStateFlow()
    
    // Store transaction details from TCP message
    private val _currentTotTrAmt = MutableStateFlow(0.0)
    private val _currentTipAmt = MutableStateFlow(0.0)
    private val _currentCurrCd = MutableStateFlow("VND")
    private val _currentTransactionType = MutableStateFlow("SALE")
    private val _currentTerminalId = MutableStateFlow("")
    private val _currentPcPosId = MutableStateFlow<String?>(null)

    // --- Filters ---
    val saleTransactions: StateFlow<List<Transaction>> = _transactionHistory
        .map { list -> list.filter { it.type == TransactionType.SALE && !it.isVoided } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val qrTransactions: StateFlow<List<Transaction>> = _transactionHistory
        .map { list -> list.filter { it.type == TransactionType.QR && !it.isVoided } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val voidTransactions: StateFlow<List<Transaction>> = _transactionHistory
        .map { list -> list.filter { it.type == TransactionType.VOID } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val refundTransactions: StateFlow<List<Transaction>> = _transactionHistory
        .map { list -> list.filter { it.type == TransactionType.REFUND } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSum: StateFlow<Double> = _transactionHistory
        .map { list -> list.sumOf { it.amount.replace(" VND", "").toDoubleOrNull() ?: 0.0 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // --- UI Methods ---
    fun updateAmount(digit: String) {
        if (digit == "del") {
            _amount.value = if (_amount.value.length > 1) _amount.value.dropLast(1) else "0"
        } else if (digit == "." && _amount.value.contains(".")) {
            return
        } else {
            _amount.value = if (_amount.value == "0") digit else _amount.value + digit
        }
    }

    fun disconnectTcp() {
        viewModelScope.launch {
            tcpService.disconnect()
        }
    }

    fun selectTip(percent: Int) {
        _selectedTip.value = percent
    }

    fun getTotalAmount(): Double {
        val base = _amount.value.toDoubleOrNull() ?: 0.0
        val tipPercent = _selectedTip.value
        return base + (base * tipPercent / 100.0)
    }

    fun reset() {
        _amount.value = "0"
        _emvCardData.value = null
        _isWaitingForNfc.value = false
        _selectedTip.value = 0
        _paymentState.value = PaymentState.Idle
        _nfcData.value = null
        _cardData.value = null
    }

    // --- Navigation & TCP Listening ---
    fun setNavController(controller: NavController) {
        navController = controller
        startListeningToTcpMessages()
    }

    private fun startListeningToTcpMessages() {
        viewModelScope.launch {
            tcpConnectionState.collect { state ->
                if (state is TcpConnectionState.DataReceived) {
                    handleIncomingTransaction(state.response)
                }
            }
        }
    }

    private fun handleIncomingTransaction(response: TransactionResponse) {
        Log.d(TAG, "Received transaction: ${response.transactionType}")
        Log.d(TAG, "Received amount: ${response.amount}")
        Log.d(TAG, "Transaction ID: ${response.transactionId}")
        Log.d(TAG, "Tip amount: ${response.tipAmt}")
        Log.d(TAG, "Currency: ${response.currCd}")
        Log.d(TAG, "Terminal ID: ${response.terminalId}")
        Log.d(TAG, "PC POS ID: ${response.pcPosId}")
        
        // Reset state trước khi nhận giao dịch mới
        reset()
        
        // Parse and store transaction details
        response.amount?.let { 
            val amountValue = it.toDoubleOrNull() ?: 0.0
            _amount.value = it
            _currentTotTrAmt.value = amountValue
            Log.d(TAG, "Amount set to: $it")
        }
        
        // Save transaction ID
        _currentTransactionId.value = response.transactionId
        Log.d(TAG, "Transaction ID saved: ${response.transactionId}")
        
        // Store transaction details from response
        _currentTransactionType.value = response.transactionType
        _currentTipAmt.value = response.tipAmt ?: 0.0
        _currentCurrCd.value = response.currCd ?: "VND"
        _currentTerminalId.value = response.terminalId ?: tcpService.getTerminalId()
        _currentPcPosId.value = response.pcPosId

        val route = when (response.transactionType.uppercase()) {
            "SALE" -> "payment"
            "QR" -> "qr"
            "VOID" -> "void"
            "REFUND" -> "refund"
            else -> null
        }

        route?.let {
            navController?.navigate(it) {
                popUpTo("home") { inclusive = false }
            }
        }
    }

    fun startTcpConnection() {
        viewModelScope.launch { tcpService.connect() }
    }

    fun retryTcpConnection() {
        viewModelScope.launch {
            tcpService.resetState()
            tcpService.connect()
        }
    }

    // --- NFC & EMV Processing ---
    fun startNfcReading() {
        _paymentState.value = PaymentState.Processing
        _isWaitingForNfc.value = true
        Log.d(TAG, "Started waiting for NFC tag...")
    }
    
    /**
     * Gửi transaction started message (msgType=2, status=STARTED)
     * Includes transactionId and pcPosId from server
     */
    fun sendTransactionStarted(amount: Double) {
        viewModelScope.launch {
            val message = TcpMessage.createTransactionStarted(
                trmId = tcpService.getTerminalId(),
                amount = String.format(Locale.US, "%.2f", amount),  // Use US locale
                transactionId = _currentTransactionId.value,
                pcPosId = _currentPcPosId.value
            )
            tcpService.sendTransactionMessage(message)
        }
    }

    fun onEmvCardRead(emvData: EmvCardData) {
        if (!_isWaitingForNfc.value) return

        _isWaitingForNfc.value = false
        _emvCardData.value = emvData
        _cardData.value = CardData.fromEmvData(emvData)
        _nfcData.value = emvData.toJson()

        onTransactionSuccess()
    }

    fun onNfcReadError(error: String) {
        if (!_isWaitingForNfc.value) return
        _isWaitingForNfc.value = false
        _paymentState.value = PaymentState.Error(error)
        onTransactionError(error)
    }

    fun onNfcTimeout() {
        _isWaitingForNfc.value = false
        _paymentState.value = PaymentState.Error("Transaction timeout")

        viewModelScope.launch {
            val message = TcpMessage(
                msgType = TcpMessage.MSG_TYPE_TRANSACTION,
                trmId = tcpService.getTerminalId(),
                status = TcpMessage.STATUS_TIMEOUT
            )
            tcpService.sendTransactionMessage(message)
        }
    }

    fun onTransactionSuccess() {
        val card = _cardData.value ?: return
        val emvData = _emvCardData.value
        val totalAmount = getTotalAmount()

        addTransaction(
            type = TransactionType.SALE,
            name = "Sale - ${card.cardScheme}",
            amount = String.format("%.2f VND", totalAmount)
        )

        viewModelScope.launch {
            val message = if (emvData != null) {
                TcpMessage.createTransactionCompleted(
                    trmId = tcpService.getTerminalId(),
                    transactionId = java.util.UUID.randomUUID().toString(),
                    emvCardData = emvData
                )
            } else {
                TcpMessage.createTransactionCompletedLegacy(
                    trmId = tcpService.getTerminalId(),
                    transactionId = java.util.UUID.randomUUID().toString(),
                    cardData = _nfcData.value ?: ""
                )
            }
//            tcpService.sendTransactionMessage(message)
            _paymentState.value = PaymentState.Approved
        }
    }

    fun onTransactionError(reason: String) {
        viewModelScope.launch {
            val message = TcpMessage.createTransactionFailed(
                trmId = tcpService.getTerminalId(),
                reason = reason
            )
            tcpService.sendTransactionMessage(message)
        }
        _paymentState.value = PaymentState.Error(reason)
    }

    // --- Transaction History Management ---
    fun addTransaction(type: TransactionType, name: String, amount: String) {
        val newTransaction = Transaction(type = type, name = name, amount = amount)
        _transactionHistory.value = _transactionHistory.value + newTransaction
    }

    fun voidTransaction(transaction: Transaction) {
        if (transaction.type == TransactionType.SALE) {
            _transactionHistory.value = _transactionHistory.value.map {
                if (it.id == transaction.id) it.copy(isVoided = true) else it
            }
            addTransaction(TransactionType.VOID, "Void - ${transaction.name}", transaction.amount)
        }
    }
    
    fun refundTransaction(transaction: Transaction) {
        if (transaction.type == TransactionType.QR) {
            _transactionHistory.value = _transactionHistory.value.map {
                if (it.id == transaction.id) it.copy(isVoided = true) else it
            }
            addTransaction(TransactionType.REFUND, "Refund - ${transaction.name}", transaction.amount)
        }
    }
    
    /**
     * Get current card data
     */
    fun getCurrentCardData(): CardData? {
        return _cardData.value
    }
    
    // ============ Bank Connector Communication Methods ============
    
    /**
     * Gửi transaction tới bank connector server qua TCP
     */
    fun sendTransactionToBankConnector(
        cardData: CardData,
        onSuccess: (TransactionResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val totalAmount = getTotalAmount()
                // Use transaction ID from server, or generate new if not available
                val transactionId = _currentTransactionId.value ?: java.util.UUID.randomUUID().toString()
                Log.d(TAG, "Sending transaction to bank with ID: $transactionId")
                
                val emvData = _emvCardData.value
                
                if (emvData == null) {
                    onError("No EMV data available")
                    return@launch
                }
                
                // Build DE55 EMV message with transaction details from TCP
                val de55 = com.example.smartpos.utils.EmvMessageBuilder.buildDE55(
                    emvData = emvData,
                    totTrAmt = _currentTotTrAmt.value,
                    tipAmt = _currentTipAmt.value,
                    currCd = _currentCurrCd.value,
                    transactionType = _currentTransactionType.value,
                    terminalId = _currentTerminalId.value,
                    transactionDate = getCurrentDate(),
                    transactionTime = getCurrentTime()
                )
                
                val message = TcpMessage(
                    msgType = TcpMessage.MSG_TYPE_TRANSACTION,
                    trmId = tcpService.getTerminalId(),
                    status = TcpMessage.STATUS_PROCESSING,
                    amount = String.format("%.2f", totalAmount),
                    transactionId = transactionId,
                    cardData = de55
                )
                
                // Check if TCP connection is available
                val currentState = tcpConnectionState.value
                if (currentState !is TcpConnectionState.Connected) {
                    Log.e(TAG, "Bank connector not connected")
                    onError("Bank connector unavailable")
                    
                    // Send failed message
                    val failedMessage = TcpMessage.createTransactionFailed(
                        trmId = tcpService.getTerminalId(),
                        reason = "Bank connector unavailable"
                    )
                    return@launch
                }
                
                // Gửi tới bank connector
                tcpService.sendToBankConnector(message)
                
                // Giả lập response từ bank connector
                // TODO: Implement actual listener for bank connector response
                delay(2000)
                
                val mockResponse = TransactionResponse(
                    transactionType = "SALE",
                    amount = String.format("%.2f", totalAmount),
                    status = "APPROVED",
                    message = "Transaction approved",
                    transactionId = transactionId
                )
                
                onSuccess(mockResponse)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending to bank connector: ${e.message}", e)
                onError("Failed: ${e.message ?: "Connection error"}")
            }
        }
    }
    
    private fun getCurrentDate(): String {
        val sdf = java.text.SimpleDateFormat("yyMMdd", java.util.Locale.US)
        return sdf.format(java.util.Date())
    }
    
    private fun getCurrentTime(): String {
        val sdf = java.text.SimpleDateFormat("HHmmss", java.util.Locale.US)
        return sdf.format(java.util.Date())
    }
    
    /**
     * Gửi QR transaction qua HTTP API tới bank connector
     */
    fun sendQRTransactionToBank(
        amount: Double,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // TODO: Implement HTTP API call to bank connector
                delay(1500)
                val mockQRCode = "QR_CODE_DATA_${System.currentTimeMillis()}"
                
                onSuccess(mockQRCode)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error creating QR transaction: ${e.message}", e)
                onError(e.message ?: "Failed to create QR code")
            }
        }
    }
}