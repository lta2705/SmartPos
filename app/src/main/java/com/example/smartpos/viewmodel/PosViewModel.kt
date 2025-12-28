package com.example.smartpos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartpos.viewmodel.PaymentState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PosViewModel : ViewModel() {
    private val _amount = MutableStateFlow("0")
    val amount = _amount.asStateFlow()

    private val _selectedTip = MutableStateFlow(0)
    val selectedTip = _selectedTip.asStateFlow()

    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState = _paymentState.asStateFlow()

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

    fun reset() {
        _amount.value = "0"
        _selectedTip.value = 0
        _paymentState.value = PaymentState.Idle
    }
}