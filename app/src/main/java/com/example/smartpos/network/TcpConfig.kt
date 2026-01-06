package com.example.smartpos.network

/**
 * TCP Configuration
 * 
 * Sửa file này để cấu hình endpoint TCP cho môi trường của bạn
 */
object TcpConfig {
    // Development Environment
    const val DEV_HOST = "192.168.1.100"
    const val DEV_PORT = 8080
    
    // Production Environment
    const val PROD_HOST = "your-production-server.com"
    const val PROD_PORT = 8443
    
    // Current Environment (thay đổi theo môi trường hiện tại)
    const val CURRENT_HOST = DEV_HOST
    const val CURRENT_PORT = DEV_PORT
    
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
}
