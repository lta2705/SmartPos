package com.example.smartpos.nfc

import android.util.Log
import com.example.smartpos.model.CardData
import com.example.smartpos.model.EmvCardData
import com.example.smartpos.utils.AtomicFlag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Sealed class for NFC reading states
 */
sealed class NfcReadState {
    object Idle : NfcReadState()
    object WaitingForCard : NfcReadState()
    data class CardRead(val emvData: EmvCardData, val cardData: CardData) : NfcReadState()
    data class Error(val message: String) : NfcReadState()
    object Timeout : NfcReadState()
}

/**
 * NFC State Manager
 * Handles NFC reading state with thread-safe atomic operations
 * Prevents race conditions when multiple NFC reads occur
 */
class NfcStateManager {
    companion object {
        private const val TAG = "NfcStateManager"
    }
    
    private val mutex = Mutex()
    private val isWaiting = AtomicFlag(false)
    
    private val _state = MutableStateFlow<NfcReadState>(NfcReadState.Idle)
    val state: StateFlow<NfcReadState> = _state.asStateFlow()
    
    private val _emvCardData = MutableStateFlow<EmvCardData?>(null)
    val emvCardData: StateFlow<EmvCardData?> = _emvCardData.asStateFlow()
    
    private val _cardData = MutableStateFlow<CardData?>(null)
    val cardData: StateFlow<CardData?> = _cardData.asStateFlow()
    
    /**
     * Start waiting for NFC card
     * Returns false if already waiting
     */
    suspend fun startWaiting(): Boolean {
        return mutex.withLock {
            // Only start if not already waiting (atomic check-and-set)
            if (isWaiting.compareAndSet(expected = false, newValue = true)) {
                _state.value = NfcReadState.WaitingForCard
                Log.d(TAG, "Started waiting for NFC card")
                true
            } else {
                Log.w(TAG, "Already waiting for NFC card")
                false
            }
        }
    }
    
    /**
     * Process EMV card read event
     * Thread-safe: only processes if we're waiting for a card
     * Returns true if the card was processed
     */
    suspend fun onCardRead(emvData: EmvCardData): Boolean {
        return mutex.withLock {
            // Only process if we were waiting (atomic check-and-set)
            if (isWaiting.compareAndSet(expected = true, newValue = false)) {
                _emvCardData.value = emvData
                _cardData.value = CardData.fromEmvData(emvData)
                _state.value = NfcReadState.CardRead(emvData, _cardData.value!!)
                Log.d(TAG, "NFC card read successfully: ${emvData.pan}")
                true
            } else {
                Log.w(TAG, "Ignoring NFC read - not waiting for card")
                false
            }
        }
    }
    
    /**
     * Handle read error
     * Thread-safe: only processes if we're waiting for a card
     */
    suspend fun onError(errorMessage: String): Boolean {
        return mutex.withLock {
            if (isWaiting.compareAndSet(expected = true, newValue = false)) {
                _state.value = NfcReadState.Error(errorMessage)
                Log.e(TAG, "NFC read error: $errorMessage")
                true
            } else {
                Log.w(TAG, "Ignoring error - not waiting for card")
                false
            }
        }
    }
    
    /**
     * Handle timeout
     */
    suspend fun onTimeout(): Boolean {
        return mutex.withLock {
            if (isWaiting.compareAndSet(expected = true, newValue = false)) {
                _state.value = NfcReadState.Timeout
                Log.w(TAG, "NFC read timeout")
                true
            } else {
                false
            }
        }
    }
    
    /**
     * Reset state to idle and clear card data
     */
    suspend fun reset() {
        mutex.withLock {
            isWaiting.set(false)
            _emvCardData.value = null
            _cardData.value = null
            _state.value = NfcReadState.Idle
            Log.d(TAG, "NFC state reset to idle")
        }
    }
    
    /**
     * Stop waiting without error
     */
    suspend fun stopWaiting() {
        mutex.withLock {
            if (isWaiting.getAndSet(false)) {
                _state.value = NfcReadState.Idle
                Log.d(TAG, "Stopped waiting for NFC card")
            }
        }
    }
    
    /**
     * Check if currently waiting for card
     */
    fun isWaitingForCard(): Boolean = isWaiting.value
    
    /**
     * Get current EMV data if available
     */
    fun getEmvData(): EmvCardData? = _emvCardData.value
    
    /**
     * Get current card data if available
     */
    fun getCardData(): CardData? = _cardData.value
}
