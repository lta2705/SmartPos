package com.example.smartpos.extensions

import com.example.smartpos.viewmodel.Transaction
import com.example.smartpos.viewmodel.TransactionType

/**
 * Extension functions for Transaction operations
 * Reduces boilerplate in ViewModels and repositories
 */

/**
 * Parse amount as Double, handling VND suffix
 */
fun Transaction.getAmountAsDouble(): Double {
    return amount.replace(" VND", "").toDoubleOrNull() ?: 0.0
}

/**
 * Check if transaction can be voided
 */
fun Transaction.canBeVoided(): Boolean {
    return type == TransactionType.SALE && !isVoided
}

/**
 * Check if transaction can be refunded
 */
fun Transaction.canBeRefunded(): Boolean {
    return type == TransactionType.QR && !isVoided
}

/**
 * Get display status string
 */
fun Transaction.getStatusText(): String {
    return when {
        isVoided -> "VOIDED"
        type == TransactionType.VOID -> "VOID"
        type == TransactionType.REFUND -> "REFUND"
        else -> "APPROVED"
    }
}

/**
 * Get short transaction ID (first 8 characters)
 */
fun Transaction.getShortId(): String {
    return id.take(8)
}

/**
 * Filter list of transactions by type
 */
fun List<Transaction>.filterByType(type: TransactionType): List<Transaction> {
    return filter { it.type == type && !it.isVoided }
}

/**
 * Calculate total amount from transaction list
 */
fun List<Transaction>.calculateTotal(): Double {
    return filter { !it.isVoided && (it.type == TransactionType.SALE || it.type == TransactionType.QR) }
        .sumOf { it.getAmountAsDouble() }
}

/**
 * Get active (non-voided) transactions
 */
fun List<Transaction>.getActive(): List<Transaction> {
    return filter { !it.isVoided }
}
