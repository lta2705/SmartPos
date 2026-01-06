package com.example.smartpos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartpos.model.CardData
import com.example.smartpos.model.TcpMessage
import com.example.smartpos.network.TcpConnectionService
import com.example.smartpos.network.TcpConnectionState
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

    // NFC Data
    private val _nfcData = MutableStateFlow<String?>(null)
    val nfcData = _nfcData.asStateFlow()

    // Parsed Card Data
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
        _selectedTip.value = 0
        _paymentState.value = PaymentState.Idle
        _nfcData.value = null
        _cardData.value = null
    }

    // ============ TCP Connection Methods ============
    
    /**
     * Bắt đầu kết nối TCP tới endpoint
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
     * Ngắt kết nối TCP
     */
    fun disconnectTcp() {
        tcpService.disconnect()
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

    /**
     * Bắt đầu đọc NFC
     */
    fun startNfcReading() {
        _paymentState.value = PaymentState.Processing
        
        // Giả lập đọc NFC sau 5 giây (thay bằng NFC thực)
        viewModelScope.launch {
            delay(5000)
            
            // Mock NFC data - format: name|card|expiry|scheme
            val mockNfcData = "JOHN DOE|**** **** **** 1234|12/25|VISA"
            simulateNfcRead(mockNfcData)
        }
    }

    /**
     * Giả lập đọc NFC (thay thế bằng NFC reader thực)
     */
    fun simulateNfcRead(nfcRawData: String) {
        _nfcData.value = nfcRawData
        
        // Parse card data
        val parsed = CardData.fromNfcData(nfcRawData)
        if (parsed != null) {
            _cardData.value = parsed
            _paymentState.value = PaymentState.Approved
        } else {
            _paymentState.value = PaymentState.Error("Invalid card data")
        }
    }

    /**
     * Xử lý NFC timeout
     */
    fun onNfcTimeout() {
        _paymentState.value = PaymentState.Error("Transaction timeout")
        
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
}