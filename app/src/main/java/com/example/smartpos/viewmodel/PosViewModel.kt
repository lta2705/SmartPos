package com.example.smartpos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class Transaction(
    val name: String,
    val amount: String
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

    // Hàm để thêm giao dịch mới
    fun addTransaction(name: String, amount: String) {
        val newTransaction = Transaction(name, amount)
        _transactionHistory.value = _transactionHistory.value + newTransaction
    }

    val totalSum: StateFlow<Double> = _transactionHistory
        .map { list -> list.sumOf { it.amount.replace(" USD", "").toDoubleOrNull() ?: 0.0 } }
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

    fun processPayment() {
        viewModelScope.launch {
            _paymentState.value = PaymentState.Processing
            delay(2500) // Giả lập thời gian quẹt thẻ
            _paymentState.value = PaymentState.Approved
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
    }
}