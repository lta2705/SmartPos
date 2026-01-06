package com.example.smartpos.viewmodel

sealed class PaymentState {
    object Idle : PaymentState()
    object Processing : PaymentState()
    object Approved : PaymentState()

    data class Error(val message: String) : PaymentState()
}