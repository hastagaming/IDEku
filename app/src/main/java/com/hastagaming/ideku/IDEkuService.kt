package com.hastagaming.ideku

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hastagaming.ideku.R // WAJIB: Agar folder drawable terbaca

class IDEkuService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "ideku_channel"
        val channelName = "IDEku Service"
        
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("IDEku")
            .setContentText("IDEku is running in the background")
            // Fix: Menggunakan drawable sesuai koordinat Komandan
            .setSmallIcon(R.drawable.ic_launcher_foreground) 
            .setOngoing(true)
            .build()

        startForeground(1, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
