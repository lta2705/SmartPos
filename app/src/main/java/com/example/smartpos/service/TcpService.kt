package com.example.smartpos.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.smartpos.data.network.TcpClientManager
import com.example.smartpos.data.repository.TcpRepository
import kotlinx.coroutines.*

class TcpService : Service() {
    private val tcpManager = TcpClientManager()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val CHANNEL_ID = "TcpServiceChannel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Smart POS Connected")
            .setContentText("Listening for transactions...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .build()

        startForeground(1, notification)
        startTcpLoop()

        return START_STICKY
    }

    private fun startTcpLoop() {
        serviceScope.launch {
            while (isActive) {
                TcpRepository.updateStatus(false)
                tcpManager.startConnection(
                    host = "127.0.0.1",
                    port = 8089,
                    onDataReceived = { data ->
                        serviceScope.launch { TcpRepository.emitMessage(data) }
                    },
                    onError = {
                        serviceScope.launch { TcpRepository.updateStatus(false) }
                    }
                )
                TcpRepository.updateStatus(true)
                delay(5000)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "TCP Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        tcpManager.close()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}