package com.example.smartpos.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Centralized date/time utilities to avoid boilerplate
 */
object DateUtils {
    
    private const val TIMESTAMP_FORMAT = "dd/MM/yyyy HH:mm:ss"
    private const val DATE_FORMAT_YYMMDD = "yyMMdd"
    private const val TIME_FORMAT_HHMMSS = "HHmmss"
    
    /**
     * Format timestamp to readable date time string
     * Used across: VoidScreen, RefundScreen, SettlementScreen, ReceiptScreen
     */
    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat(TIMESTAMP_FORMAT, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    /**
     * Get current date in YYMMDD format for EMV messages
     */
    fun getCurrentDate(): String {
        val sdf = SimpleDateFormat(DATE_FORMAT_YYMMDD, Locale.US)
        return sdf.format(Date())
    }
    
    /**
     * Get current time in HHMMSS format for EMV messages
     */
    fun getCurrentTime(): String {
        val sdf = SimpleDateFormat(TIME_FORMAT_HHMMSS, Locale.US)
        return sdf.format(Date())
    }
    
    /**
     * Get current date time for receipts
     */
    fun getCurrentDateTime(): String {
        return formatTimestamp(System.currentTimeMillis())
    }
}
