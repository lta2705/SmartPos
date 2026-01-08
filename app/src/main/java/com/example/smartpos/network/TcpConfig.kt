package com.example.smartpos.network

/**
 * TCP Configuration
 * 
 * Sửa file này để cấu hình endpoint TCP cho môi trường của bạn
 */
object TcpConfig {
    // Development Environment
    const val DEV_HOST = "10.0.2.2"
    const val DEV_PORT = 8089
    
    // Production Environment
    const val PROD_HOST = "192.168.100.174"
    const val PROD_PORT = 8089
    
    // Current Environment (thay đổi theo môi trường hiện tại)
    const val CURRENT_HOST = PROD_HOST
    const val CURRENT_PORT = PROD_PORT
    
    // Timeout Settings
    const val SOCKET_TIMEOUT_MS = 30000  // 30 seconds
    const val CONNECT_TIMEOUT_MS = 15000 // 15 seconds
    const val READ_TIMEOUT_MS = 30000    // 30 seconds
    
    // Keep-alive Settings
    const val KEEP_ALIVE_ENABLED = true
    const val KEEP_ALIVE_INTERVAL_MS = 5000L // 5 seconds
    
    // Retry Settings
    const val MAX_RETRY_ATTEMPTS = 3
    const val RETRY_DELAY_MS = 2000L // 2 seconds
    
    // Bank Connector Server (will be provided later)
    const val BANK_CONNECTOR_HOST = "10.0.2.2"  // TODO: Update with actual bank connector host
    const val BANK_CONNECTOR_PORT = 8888         // TODO: Update with actual bank connector port
    const val BANK_CONNECTOR_HTTP_URL = "http://10.0.2.2:9090" // For QR HTTP API
}
