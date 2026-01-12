package com.example.smartpos.repository

import com.example.smartpos.viewmodel.Transaction
import com.example.smartpos.viewmodel.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Repository for managing transaction data
 * Provides thread-safe operations on transaction history
 */
class TransactionRepository {
    private val mutex = Mutex()
    
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()
    
    /**
     * Add a new transaction
     */
    suspend fun addTransaction(transaction: Transaction) {
        mutex.withLock {
            _transactions.value = _transactions.value + transaction
        }
    }
    
    /**
     * Add a transaction with parameters
     */
    suspend fun addTransaction(type: TransactionType, name: String, amount: String): Transaction {
        val transaction = Transaction(type = type, name = name, amount = amount)
        addTransaction(transaction)
        return transaction
    }
    
    /**
     * Mark a transaction as voided
     */
    suspend fun markAsVoided(transactionId: String): Boolean {
        return mutex.withLock {
            val index = _transactions.value.indexOfFirst { it.id == transactionId }
            if (index >= 0) {
                _transactions.value = _transactions.value.mapIndexed { i, t ->
                    if (i == index) t.copy(isVoided = true) else t
                }
                true
            } else {
                false
            }
        }
    }
    
    /**
     * Find transaction by ID
     */
    fun findById(transactionId: String): Transaction? {
        return _transactions.value.find { it.id == transactionId }
    }
    
    /**
     * Get all transactions of a specific type
     */
    fun getByType(type: TransactionType): List<Transaction> {
        return _transactions.value.filter { it.type == type }
    }
    
    /**
     * Get all non-voided transactions of a specific type
     */
    fun getActiveByType(type: TransactionType): List<Transaction> {
        return _transactions.value.filter { it.type == type && !it.isVoided }
    }
    
    /**
     * Calculate total sum of all transactions
     * SALE: adds to total
     * VOID: subtracts (original SALE amount)
     * REFUND: subtracts
     */
    fun calculateTotal(): Double {
        return _transactions.value.sumOf { transaction ->
            val amount = parseAmount(transaction.amount)
            when (transaction.type) {
                TransactionType.SALE -> if (transaction.isVoided) 0.0 else amount
                TransactionType.QR -> if (transaction.isVoided) 0.0 else amount
                TransactionType.VOID -> -amount  // VOID subtracts
                TransactionType.REFUND -> -amount  // REFUND subtracts
                TransactionType.SETTLEMENT -> 0.0
            }
        }
    }
    
    /**
     * Clear all transactions
     */
    suspend fun clear() {
        mutex.withLock {
            _transactions.value = emptyList()
        }
    }
    
    /**
     * Parse amount string to Double
     * Handles formats like "100.00 VND", "100.00", etc.
     */
    private fun parseAmount(amountStr: String): Double {
        return amountStr
            .replace(Regex("[^0-9.]"), "")
            .toDoubleOrNull() ?: 0.0
    }
}
