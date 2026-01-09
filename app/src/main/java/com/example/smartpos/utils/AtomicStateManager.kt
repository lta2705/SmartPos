package com.example.smartpos.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Thread-safe state manager using Mutex for atomic operations on StateFlow
 * Prevents race conditions when multiple coroutines access the same state
 */
class AtomicStateManager<T>(initialValue: T) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow(initialValue)
    val state: StateFlow<T> = _state.asStateFlow()
    
    val value: T get() = _state.value
    
    /**
     * Thread-safe update with transformation function
     */
    suspend fun update(transform: (T) -> T) {
        mutex.withLock {
            _state.value = transform(_state.value)
        }
    }
    
    /**
     * Thread-safe set value
     */
    suspend fun set(newValue: T) {
        mutex.withLock {
            _state.value = newValue
        }
    }
    
    /**
     * Thread-safe compare and set
     * Returns true if the value was updated
     */
    suspend fun compareAndSet(expected: T, newValue: T): Boolean {
        return mutex.withLock {
            if (_state.value == expected) {
                _state.value = newValue
                true
            } else {
                false
            }
        }
    }
    
    /**
     * Thread-safe get and set
     * Returns the old value
     */
    suspend fun getAndSet(newValue: T): T {
        return mutex.withLock {
            val oldValue = _state.value
            _state.value = newValue
            oldValue
        }
    }
}

/**
 * Thread-safe boolean flag using AtomicBoolean
 * More efficient than AtomicStateManager for simple boolean flags
 */
class AtomicFlag(initialValue: Boolean = false) {
    private val flag = AtomicBoolean(initialValue)
    
    val value: Boolean get() = flag.get()
    
    fun set(newValue: Boolean) {
        flag.set(newValue)
    }
    
    fun compareAndSet(expected: Boolean, newValue: Boolean): Boolean {
        return flag.compareAndSet(expected, newValue)
    }
    
    fun getAndSet(newValue: Boolean): Boolean {
        return flag.getAndSet(newValue)
    }
}

/**
 * Thread-safe nullable reference using AtomicReference
 */
class AtomicNullable<T>(initialValue: T? = null) {
    private val reference = AtomicReference<T?>(initialValue)
    
    val value: T? get() = reference.get()
    
    fun set(newValue: T?) {
        reference.set(newValue)
    }
    
    fun compareAndSet(expected: T?, newValue: T?): Boolean {
        return reference.compareAndSet(expected, newValue)
    }
    
    fun getAndSet(newValue: T?): T? {
        return reference.getAndSet(newValue)
    }
    
    /**
     * Get value or throw if null
     */
    fun getOrThrow(message: String = "Value is null"): T {
        return reference.get() ?: throw IllegalStateException(message)
    }
    
    /**
     * Get value or return default
     */
    fun getOrDefault(default: T): T {
        return reference.get() ?: default
    }
}
