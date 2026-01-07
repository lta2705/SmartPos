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

    // TCP Connection Service
    private val tcpService = TcpConnectionService()
    val tcpConnectionState: StateFlow<TcpConnectionState> = tcpService.connectionState

    // EMV Data (full)
    private val _emvCardData = MutableStateFlow<EmvCardData?>(null)
    val emvCardData = _emvCardData.asStateFlow()

    // NFC Data (legacy - for backward compatibility)
    private val _nfcData = MutableStateFlow<String?>(null)
    val nfcData = _nfcData.asStateFlow()

    // Parsed Card Data (for UI display)
    private val _cardData = MutableStateFlow<CardData?>(null)
    val cardData = _cardData.asStateFlow()

    // Lọc giao dịch theo loại
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

    // Hàm để thêm giao dịch mới
    fun addTransaction(type: TransactionType, name: String, amount: String) {
        val newTransaction = Transaction(
            type = type,
            name = name,
            amount = amount
        )
        _transactionHistory.value = _transactionHistory.value + newTransaction
    }

    // Void một giao dịch Sale
    fun voidTransaction(transaction: Transaction) {
        if (transaction.type == TransactionType.SALE) {
            // Đánh dấu giao dịch là voided
            _transactionHistory.value = _transactionHistory.value.map {
                if (it.id == transaction.id) it.copy(isVoided = true) else it
            }
            // Tạo bản ghi Void
            addTransaction(
                type = TransactionType.VOID,
                name = "Void - ${transaction.name}",
                amount = transaction.amount
            )
        }
    }

    // Refund một giao dịch QR
    fun refundTransaction(transaction: Transaction) {
        if (transaction.type == TransactionType.QR) {
            // Đánh dấu giao dịch là voided
            _transactionHistory.value = _transactionHistory.value.map {
                if (it.id == transaction.id) it.copy(isVoided = true) else it
            }
            // Tạo bản ghi Refund
            addTransaction(
                type = TransactionType.REFUND,
                name = "Refund - ${transaction.name}",
                amount = transaction.amount
            )
        }
    }

    val totalSum: StateFlow<Double> = _transactionHistory
        .map { list -> list.sumOf { it.amount.replace(" VND", "").toDoubleOrNull() ?: 0.0 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun updateAmount(digit: String) {
        if (digit == "del") {
            _amount.value = if (_amount.value.length > 1) _amount.value.dropLast(1) else "0"
        } else if (digit == "." && _amount.value.contains(".")) {
            return
        } else {
            _amount.value = if (_amount.value == "0") digit else _amount.value + digit
        }
    }

    fun selectTip(percent: Int) {
        _selectedTip.value = percent
    }

    fun processNfcPayment(tagId: String) {
        viewModelScope.launch {
            _paymentState.value = PaymentState.Processing

            delay(2000)

            _paymentState.value = PaymentState.Approved
        }
    }

    fun processPayment() {
        if (_paymentState.value is PaymentState.Processing) return

        viewModelScope.launch {
            try {
                _paymentState.value = PaymentState.Processing

                // 1. Kiểm tra logic nghiệp vụ cơ bản
                val currentAmount = amount.value.toDoubleOrNull() ?: 0.0
                if (currentAmount <= 0) {
                    throw Exception("Số tiền không hợp lệ")
                }

                // 2. Giả lập gọi API lên Server ngân hàng
                // Trong thực tế, bạn sẽ dùng Retrofit: val response = repository.makePayment(tagId, currentAmount)
                val response = true

                if (response) {
                    _paymentState.value = PaymentState.Approved
                } else {
                    _paymentState.value = PaymentState.Error("Thẻ bị từ chối hoặc số dư không đủ")
                }

            } catch (e: Exception) {
                // 3. Bắt các lỗi hệ thống (Mất mạng, lỗi phần cứng NFC, lỗi logic)
                _paymentState.value = PaymentState.Error(e.message ?: "Lỗi giao dịch không xác định")
            }
        }
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

    // ============ TCP Connection Methods ============
    
    /**
     * Set navigation controller để có thể navigate từ ViewModel
     */
    fun setNavController(controller: NavController) {
        navController = controller
        startListeningToTcpMessages()
    }
    
    /**
     * Lắng nghe TCP messages và route theo TransactionType
     */
    private fun startListeningToTcpMessages() {
        viewModelScope.launch {
            tcpConnectionState.collect { state ->
                if (state is TcpConnectionState.DataReceived) {
                    handleIncomingTransaction(state.response)
                }
            }
        }
    }
    
    /**
     * Xử lý transaction nhận được từ TCP socket
     */
    private fun handleIncomingTransaction(response: TransactionResponse) {
        Log.d(TAG, "Received transaction: ${response.transactionType}")
        
        when (response.transactionType.uppercase()) {
            "SALE" -> {
                // Reset và set amount từ server
                response.amount?.let { 
                    _amount.value = it
                }
                
                // Navigate tới Payment screen
                navController?.navigate("payment") {
                    popUpTo("home") { inclusive = false }
                }
            }
            "QR" -> {
                // Reset và set amount từ server
                response.amount?.let { 
                    _amount.value = it
                }
                
                // Navigate tới QR screen
                navController?.navigate("qr") {
                    popUpTo("home") { inclusive = false }
                }
            }
            "VOID" -> {
                navController?.navigate("void") {
                    popUpTo("home") { inclusive = false }
                }
            }
            "REFUND" -> {
                navController?.navigate("refund") {
                    popUpTo("home") { inclusive = false }
                }
            }
            else -> {
                Log.w(TAG, "Unknown transaction type: ${response.transactionType}")
            }
        }
    }
    
    /**
     * Bắt đầu kết nối TCP tới endpoint với auto-retry
     * Được gọi 1 lần duy nhất khi app khởi động
     */
    fun startTcpConnection() {
        viewModelScope.launch {
            tcpService.connect()
        }
    }

    /**
     * Thử lại kết nối TCP
     */
    fun retryTcpConnection() {
        viewModelScope.launch {
            tcpService.resetState()
            tcpService.connect()
        }
    }

    /**
     * Ngắt kết nối TCP (không nên gọi trừ khi cần thiết)
     */
    fun disconnectTcp() {
        // Không disconnect vì cần maintain long-lived connection
        // tcpService.disconnect()
    }

    /**
     * Gửi dữ liệu qua TCP (nếu cần)
     */
    fun sendTcpData(data: String) {
        viewModelScope.launch {
            tcpService.sendData(data)
        }
    }

    /**
     * Reset TCP state về Idle
     */
    fun resetTcpState() {
        tcpService.resetState()
    }

    // ============ NFC & Card Processing Methods ============

    /**
     * Gửi transaction started message (msgType=2, status=STARTED)
     */
    fun sendTransactionStarted(amount: Double) {
        viewModelScope.launch {
            val message = TcpMessage.createTransactionStarted(
                trmId = tcpService.getTerminalId(),
                amount = String.format("%.2f", amount)
            )
            tcpService.sendTransactionMessage(message)
        }
    }

    // NFC reading state
    private val _isWaitingForNfc = MutableStateFlow(false)
    val isWaitingForNfc = _isWaitingForNfc.asStateFlow()

    /**
     * Bắt đầu đọc NFC - đợi real NFC tag
     */
    fun startNfcReading() {
        _paymentState.value = PaymentState.Processing
        _isWaitingForNfc.value = true
        
        Log.d(TAG, "Started waiting for NFC tag...")
    }
    
    /**
     * Stop waiting for NFC (khi timeout hoặc cancel)
     */
    fun stopNEMV card data từ real NFC tag
     */
    fun onEmvCardRead(emvData: EmvCardData) {
        // Only process if we're waiting for NFC
        if (!_isWaitingForNfc.value) {
            Log.w(TAG, "Received EMV data but not in waiting state")
            return
        }
        
        Log.d(TAG, "EMV Card Read: ${emvData.rawTlvData.size} TLV tags")
        _isWaitingForNfc.value = false
        _emvCardData.value = emvData
        
        // Create CardData for UI display from EMV data
        val displayCardData = CardData.fromEmvData(emvData)
        _cardData.value = displayCardData
        
        // Set NFC data as JSON for logging
        _nfcData.value = emvData.toJson()
        
        _paymentState.value = PaymentState.Approved
        Log.d(TAG, "EMV Card parsed successfully: ${displayCardData.cardScheme} - ${displayCardData.maskedCardNumber}")
    }
    
    /**
     * Xử lý NFC read error
     */
    fun onNfcReadError(error: String) {
        if (!_isWaitingForNfc.value) {
            return
        }
        
        _isWaitingForNfc.value = false
        _paymentState.value = PaymentState.Error(error)
        Log.e(TAG, "NFC Read Error: $error")
    }
    
    /**
     * Xử lý NFC data từ real NFC tag (legacy - for backward compatibility)
     */
    @Deprecated("Use onEmvCardRead instead") _isWaitingForNfc.value = false
        Log.d(TAG, "Stopped waiting for NFC tag")
    }

    /**
     * Xử lý NFC data từ real NFC tag
     */
    fun onNfcTagRead(nfcRawData: String) {
        // Only process if we're waiting for NFC
        if (!_isWaitingForNfc.value) {
            emvData = _emvCardData.value
        val totalAmount = getTotalAmount()
        
        // Lưu transaction vào history
        addTransaction(
            type = TransactionType.SALE,
            name = "Sale - ${card.cardScheme}",
            amount = String.format("%.2f VND", totalAmount)
        )
        
        // Gửi completed message tới server với EMV data
        viewModelScope.launch {
            val message = if (emvData != null) {
                // Send with full EMV data and Field 55
                TcpMessage.createTransactionCompleted(
                    trmId = tcpService.getTerminalId(),
                    transactionId = java.util.UUID.randomUUID().toString(),
                    emvCardData = emvData
                )
            } else {
                // Fallback to legacy format
                @Suppress("DEPRECATION")
                TcpMessage.createTransactionCompletedLegacy(
                    trmId = tcpService.getTerminalId(),
                    transactionId = java.util.UUID.randomUUID().toString(),
                    cardData = _nfcData.value ?: ""
                )
            }
            
            tcpService.sendTransactionMessage(message)
            
            Log.d(TAG, "Transaction completed message sent")
            if (emvData != null) {
                Log.d(TAG, "Field 55: ${emvData.packField55()}")
            }: $nfcRawData")
        }
    }

    /**
     * Xử lý NFC timeout
     */
    fun onNfcTimeout() {
        _isWaitingForNfc.value = false
        _paymentState.value = PaymentState.Error("Transaction timeout")
        
        Log.d(TAG, "NFC read timeout")
        
        // Gửi timeout message tới server
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
     * Xử lý khi transaction thành công
     */
    fun onTransactionSuccess() {
        val card = _cardData.value ?: return
        val totalAmount = getTotalAmount()
        
        // Lưu transaction vào history
        addTransaction(
            type = TransactionType.SALE,
            name = "Sale - ${card.cardScheme}",
            amount = String.format("%.2f VND", totalAmount)
        )
        
        // Gửi completed message tới server
        viewModelScope.launch {
            val message = TcpMessage.createTransactionCompleted(
                trmId = tcpService.getTerminalId(),
                transactionId = java.util.UUID.randomUUID().toString(),
                cardData = _nfcData.value ?: ""
            )
            tcpService.sendTransactionMessage(message)
        }
        
        _paymentState.value = PaymentState.Approved
    }

    /**
     * Xử lý khi transaction thất bại
     */
    fun onTransactionError(reason: String) {
        // Gửi failed message tới server
        viewModelScope.launch {
            val message = TcpMessage.createTransactionFailed(
                trmId = tcpService.getTerminalId(),
                reason = reason
            )
            tcpService.sendTransactionMessage(message)
        }
        
        _paymentState.value = PaymentState.Error(reason)
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
                // Tạo message gửi tới bank connector
                val totalAmount = getTotalAmount()
                val transactionId = java.util.UUID.randomUUID().toString()
                
                val message = TcpMessage(
                    msgType = TcpMessage.MSG_TYPE_TRANSACTION,
                    trmId = tcpService.getTerminalId(),
                    status = TcpMessage.STATUS_PROCESSING,
                    amount = String.format("%.2f", totalAmount),
                    transactionId = transactionId,
                    cardData = _nfcData.value ?: ""
                )
                
                // Gửi tới bank connector
                tcpService.sendToBankConnector(message)
                
                // Giả lập response từ bank connector (thực tế sẽ nhận từ socket)
                // TODO: Implement actual listener for bank connector response
                delay(2000)
                
                // Mock response - thực tế sẽ parse từ TCP socket
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
                onError(e.message ?: "Failed to communicate with bank")
            }
        }
    }
    
    /**
     * Gửi QR transaction qua HTTP API tới bank connector
     */
    fun sendQRTransactionToBank(
        amount: Double,
        onSuccess: (String) -> Unit, // Returns QR code data
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // TODO: Implement HTTP API call to bank connector
                // val response = httpClient.post(\"${TcpConfig.BANK_CONNECTOR_HTTP_URL}/qr\") {
                //     body = QRRequest(amount, terminalId)
                // }
                
                // Giả lập QR response
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