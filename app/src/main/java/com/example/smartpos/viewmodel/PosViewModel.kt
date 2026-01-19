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
import com.example.smartpos.utils.AmountUtils
import com.example.smartpos.utils.DateUtils
import com.example.smartpos.extensions.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

enum class TransactionType {
    SALE, QR, VOID, REFUND,SETTLEMENT
}

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val type: TransactionType,
    val name: String,
    val amount: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isVoided: Boolean = false,
    val emvData: EmvCardData? = null  // Store EMV data for void/refund operations
)

class PosViewModel : ViewModel() {
    companion object {
        private const val TAG = "PosViewModel"
    }
    
    // Mutex for thread-safe state updates
    private val stateMutex = Mutex()
    
    // Navigation controller reference (weak reference to avoid memory leak)
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
        .map { list -> list.calculateTotal() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // --- UI Methods ---
    fun updateAmount(digit: String) {
        _amount.update { current ->
            when {
                digit == "del" -> if (current.length > 1) current.dropLast(1) else "0"
                digit == "." && current.contains(".") -> current  // Don't add second decimal
                current == "0" && digit != "." -> digit
                else -> current + digit
            }
        }
    }
    
    /**
     * Clear transaction state when returning from payment/QR screens
     * Called by onReturn navigation callback
     * Thread-safe state clearing
     */
    fun clearTransactionState() {
        Log.d(TAG, "Clearing transaction state on return")
        viewModelScope.launch {
            stateMutex.withLock {
                _amount.value = "0"
                _selectedTip.value = 0
                _currentTransactionId.value = null
                _currentPcPosId.value = null
                _currentTotTrAmt.value = 0.0
                _currentTipAmt.value = 0.0
                _currentCurrCd.value = "VND"
                _currentTransactionType.value = "SALE"
                _currentTerminalId.value = ""
                _emvCardData.value = null
                _cardData.value = null
                _isWaitingForNfc.value = false
                _paymentState.value = PaymentState.Idle
            }
        }
    }

    fun disconnectTcp() {
        tcpService.disconnect()
    }

    fun selectTip(percent: Int) {
        _selectedTip.value = percent
    }

    fun getTotalAmount(): Double {
        val base = _amount.value.toDoubleOrNull() ?: 0.0
        return AmountUtils.calculateWithTip(base, _selectedTip.value)
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
                amount = AmountUtils.formatAmountOnly(amount),
                transactionId = _currentTransactionId.value,
                pcPosId = _currentPcPosId.value
            )
            tcpService.sendTransactionMessage(message)
        }
    }

    /**
     * Process EMV card read with thread-safe NFC state check
     * Prevents race condition when multiple NFC reads occur
     */
    fun onEmvCardRead(emvData: EmvCardData) {
        viewModelScope.launch {
            // Atomic check-and-set to prevent race condition
            val wasWaiting = stateMutex.withLock {
                if (_isWaitingForNfc.value) {
                    _isWaitingForNfc.value = false
                    true
                } else {
                    false
                }
            }
            
            if (!wasWaiting) {
                Log.w(TAG, "Ignoring NFC read - not waiting for card")
                return@launch
            }

            _emvCardData.value = emvData
            _cardData.value = CardData.fromEmvData(emvData)
            _nfcData.value = emvData.toJson()

            onTransactionSuccess()
        }
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

    /**
     * Handle successful transaction
     * Null-safe with proper logging on failure
     */
    fun onTransactionSuccess() {
        val card = _cardData.value
        if (card == null) {
            Log.w(TAG, "onTransactionSuccess called but card data is null")
            return
        }
        
        val emvData = _emvCardData.value
        val totalAmount = getTotalAmount()

        // Add transaction with EMV data
        val newTransaction = Transaction(
            type = TransactionType.SALE,
            name = "Sale - ${card.cardScheme}",
            amount = AmountUtils.formatAmount(totalAmount, "VND"),
            emvData = emvData
        )
        _transactionHistory.update { current -> current + newTransaction }

        viewModelScope.launch {
            // Generate transaction ID safely
            val transactionId = _currentTransactionId.value ?: UUID.randomUUID().toString()
            
            if (emvData != null) {
                @Suppress("UNUSED_VARIABLE")
                val message = TcpMessage.createTransactionCompleted(
                    trmId = tcpService.getTerminalId(),
                    transactionId = transactionId,
                    emvCardData = emvData
                )
                // Uncomment to send message: tcpService.sendTransactionMessage(message)
            }
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
    /**
     * Add transaction with thread-safe list update
     */
    fun addTransaction(type: TransactionType, name: String, amount: String, emvData: EmvCardData? = null) {
        val newTransaction = Transaction(type = type, name = name, amount = amount, emvData = emvData)
        _transactionHistory.update { current -> current + newTransaction }
    }

    fun voidTransaction(transaction: Transaction) {
        if (transaction.type != TransactionType.SALE) {
            Log.w(TAG, "Cannot void non-SALE transaction: ${transaction.type}")
            return
        }
        
        viewModelScope.launch {
            try {
                // Use EMV data from transaction instead of current state
                val emvData = transaction.emvData
                if (emvData == null) {
                    Log.e(TAG, "No EMV data available for VOID - transaction: ${transaction.id}")
                    return@launch
                }
                
                // Parse amount from transaction
                val amount = transaction.getAmountAsDouble()
                
                // Build DE55 EMV message for VOID
                val de55 = com.example.smartpos.utils.EmvMessageBuilder.buildDE55(
                    emvData = emvData,
                    totTrAmt = amount,
                    tipAmt = 0.0,
                    currCd = "VND",
                    transactionType = "VOID",
                    terminalId = _currentTerminalId.value.ifEmpty { tcpService.getTerminalId() },
                    transactionDate = DateUtils.getCurrentDate(),
                    transactionTime = DateUtils.getCurrentTime()
                )
                
                val voidMessage = TcpMessage(
                    msgType = TcpMessage.MSG_TYPE_TRANSACTION,
                    trmId = tcpService.getTerminalId(),
                    status = TcpMessage.STATUS_PROCESSING,
                    amount = AmountUtils.formatAmountOnly(amount).toBigDecimal(),
                    transactionId = transaction.id,
                    cardData = de55,
                    transactionType = TransactionType.VOID
                )
                
                // Send to bank connector
                tcpService.sendToBankConnector(voidMessage)
                Log.d(TAG, "VOID transaction sent to bank connector for ${transaction.id}")
                
                // Mark transaction as voided (thread-safe update)
                _transactionHistory.update { list ->
                    list.map { if (it.id == transaction.id) it.copy(isVoided = true) else it }
                }
                // Create VOID transaction with EMV data
                addTransaction(TransactionType.VOID, "Void - ${transaction.name}", transaction.amount, emvData)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending VOID to bank: ${e.message}", e)
            }
        }
    }
    
    fun refundTransaction(transaction: Transaction) {
        if (transaction.type != TransactionType.QR) {
            Log.w(TAG, "Cannot refund non-QR transaction: ${transaction.type}")
            return
        }
        
        viewModelScope.launch {
            try {
                // Use EMV data from transaction instead of current state
                val emvData = transaction.emvData
                if (emvData == null) {
                    Log.e(TAG, "No EMV data available for REFUND - transaction: ${transaction.id}")
                    return@launch
                }
                
                // Parse amount from transaction
                val amount = transaction.getAmountAsDouble()
                
                // Build DE55 EMV message for REFUND
                val de55 = com.example.smartpos.utils.EmvMessageBuilder.buildDE55(
                    emvData = emvData,
                    totTrAmt = amount,
                    tipAmt = 0.0,
                    currCd = "VND",
                    transactionType = "REFUND",
                    terminalId = _currentTerminalId.value.ifEmpty { tcpService.getTerminalId() },
                    transactionDate = DateUtils.getCurrentDate(),
                    transactionTime = DateUtils.getCurrentTime()
                )
                
                val refundMessage = TcpMessage(
                    msgType = TcpMessage.MSG_TYPE_TRANSACTION,
                    trmId = tcpService.getTerminalId(),
                    status = TcpMessage.STATUS_PROCESSING,
                    amount = AmountUtils.formatAmountOnly(amount).toBigDecimal(),
                    transactionId = transaction.id,
                    cardData = de55,
                    transactionType = TransactionType.REFUND
                )
                
                // Send to bank connector
                tcpService.sendToBankConnector(refundMessage)
                Log.d(TAG, "REFUND transaction sent to bank connector for ${transaction.id}")
                
                // Mark transaction as voided (refunded) - thread-safe
                _transactionHistory.update { list ->
                    list.map { if (it.id == transaction.id) it.copy(isVoided = true) else it }
                }
                // Create REFUND transaction with EMV data
                addTransaction(TransactionType.REFUND, "Refund - ${transaction.name}", transaction.amount, emvData)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending REFUND to bank: ${e.message}", e)
            }
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
     * Fixed null safety for transaction ID
     * @param _unused Previously cardData, now uses emvCardData from state
     */
    @Suppress("UNUSED_PARAMETER")
    fun sendTransactionToBankConnector(
        onSuccess: (TransactionResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val totalAmount = getTotalAmount()
                
                // Safe transaction ID handling - capture value once to avoid race condition
                val serverTransactionId = _currentTransactionId.value
                val transactionId = serverTransactionId ?: run {
                    val randomId = UUID.randomUUID().toString()
                    Log.d(TAG, "Generated random transaction ID for keyboard entry: $randomId")
                    randomId
                }
                Log.d(TAG, "Sending transaction to bank with ID: $transactionId")
                
                val emvData = _emvCardData.value
                
                if (emvData == null) {
                    onError("No EMV data available")
                    return@launch
                }
                
                // Build DE55 EMV message with transaction details from TCP
                // Use total amount if server amount is 0
                val effectiveAmount = _currentTotTrAmt.value.takeIf { it > 0.0 } ?: totalAmount
                val terminalId = _currentTerminalId.value.ifEmpty { tcpService.getTerminalId() }
                
                val de55 = com.example.smartpos.utils.EmvMessageBuilder.buildDE55(
                    emvData = emvData,
                    totTrAmt = effectiveAmount,
                    tipAmt = _currentTipAmt.value,
                    currCd = _currentCurrCd.value,
                    transactionType = _currentTransactionType.value,
                    terminalId = terminalId,
                    transactionDate = DateUtils.getCurrentDate(),
                    transactionTime = DateUtils.getCurrentTime()
                )
                
                val message = TcpMessage(
                    msgType = TcpMessage.MSG_TYPE_TRANSACTION,
                    trmId = tcpService.getTerminalId(),
                    status = TcpMessage.STATUS_PROCESSING,
                    amount = AmountUtils.formatAmountOnly(totalAmount).toBigDecimal(),
                    transactionId = transactionId,
                    cardData = de55,
                    transactionType = TransactionType.SALE
                )
                
                // Check if TCP connection is available
                if (!tcpService.isConnected()) {
                    Log.e(TAG, "Main TCP not connected")
                    onError("Server unavailable")
                    return@launch
                }
                
                // Gửi tới bank connector
                tcpService.sendToBankConnector(message)
                
                // Giả lập response từ bank connector
                // TODO: Implement actual listener for bank connector response
                delay(2000)
                
                val mockResponse = TransactionResponse(
                    transactionType = "SALE",
                    amount = AmountUtils.formatAmountOnly(totalAmount),
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
    
    // Removed duplicate date/time methods - now using DateUtils
    
    // Settlement state
    private val _isSettling = MutableStateFlow(false)
    val isSettling: StateFlow<Boolean> = _isSettling.asStateFlow()
    
    /**
     * Perform settlement - send settlement request to bank connector
     */
    fun performSettlement() {
        viewModelScope.launch {
            try {
                _isSettling.value = true
                Log.d(TAG, "Performing settlement...")
                
                // Build settlement message
                val settlementMessage = TcpMessage(
                    msgType = TcpMessage.MSG_TYPE_TRANSACTION,
                    trmId = tcpService.getTerminalId(),
                    status = TcpMessage.STATUS_PROCESSING,
                    transactionType = TransactionType.SETTLEMENT
                )
                
                // Send to bank connector
                tcpService.sendToBankConnector(settlementMessage)
                Log.d(TAG, "Settlement request sent to bank connector")
                
                // Simulate settlement processing time
                delay(2000)
                
                // Clear all transaction data after successful settlement
                _transactionHistory.value = emptyList()
                Log.d(TAG, "Settlement completed and data cleared")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error performing settlement: ${e.message}", e)
            } finally {
                _isSettling.value = false
            }
        }
    }
    
    /**
     * Gửi QR transaction qua HTTP API tới bank connector
     * Không kèm dữ liệu EMV - chỉ gửi thông tin giao dịch cơ bản
     */
    fun sendQRTransactionToBank(
        amount: Double,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Sending QR transaction to bank connector via HTTP")
                
                // Lấy thông tin từ TCP message
                val transactionId = _currentTransactionId.value ?: UUID.randomUUID().toString()
                val terminalId = _currentTerminalId.value.ifEmpty { tcpService.getTerminalId() }
                val pcPosId = _currentPcPosId.value
                
                // Tạo URL request
                val url = java.net.URL("${com.example.smartpos.network.TcpConfig.BANK_CONNECTOR_HTTP_URL}/create_qr")
                val connection = withContext(Dispatchers.IO) {
                    url.openConnection() as java.net.HttpURLConnection
                }
                
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                }
                
                // Tạo JSON request body (không có EMV data)
                val requestBody = JSONObject().apply {
                    put("transactionId", transactionId)
                    put("terminalId", terminalId)
                    put("amount", amount)
                    put("currency", _currentCurrCd.value)
                    put("transactionType", "QR")
                    pcPosId?.let { put("pcPosId", it) }
                }
                
                Log.d(TAG, "QR Request: $requestBody")
                
                // Gửi request
                withContext(Dispatchers.IO) {
                    connection.outputStream.use { os ->
                        os.write(requestBody.toString().toByteArray())
                    }
                    
                    // Đọc response
                    val responseCode = connection.responseCode
                    Log.d(TAG, "QR Response code: $responseCode")
                    
                    if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        Log.d(TAG, "QR Response: $response")
                        
                        // Parse response JSON để lấy QR code text
                        val responseJson = JSONObject(response)
                        val qrCodeData = responseJson.optString("qrCode") 
                            ?: responseJson.optString("qrData")
                            ?: responseJson.optString("data")
                            ?: response // Fallback to full response if no specific field
                        
                        onSuccess(qrCodeData)
                    } else {
                        val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                        Log.e(TAG, "QR Error response: $errorResponse")
                        onError("Failed to create QR code: HTTP $responseCode")
                    }
                    
                    connection.disconnect()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error creating QR transaction: ${e.message}", e)
                onError(e.message ?: "Failed to create QR code")
            }
        }
    }
}