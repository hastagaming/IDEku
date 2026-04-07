package com.hastagaming.ideku

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class IDEkuService : Service() {

    private val CHANNEL_ID = "ideku_service_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Membuat Notifikasi agar Android tidak mematikan aplikasi
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("IDEku Terminal is Active!")
            .setContentText("IDEku is running in background...")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Pakai ikon sistem sementara
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // Menjalankan Foreground Service
        startForeground(1, notification)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "IDEku Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}
