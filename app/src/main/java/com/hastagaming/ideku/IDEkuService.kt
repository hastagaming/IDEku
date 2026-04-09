package com.hastagaming.ideku

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlin.system.exitProcess

class IDEkuService : Service() {

    private val CHANNEL_ID = "ideku_service_channel"
    private val NOTIFICATION_ID = 1337
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val ACTION_STOP_SERVICE = "STOP_IDEku_Service"
        const val ACTION_WAKELOCK_TOGGLE = "Activate_Wakelock"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_STOP_SERVICE -> {
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                exitProcess(0) // Benar-benar keluar dari sistem
            }
            ACTION_WAKELOCK_TOGGLE -> {
                toggleWakeLock()
                // Update notifikasi agar teks tombol berubah
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, buildNotification())
            }
            else -> {
                // Jalankan Foreground Service pertama kali
                startForeground(NOTIFICATION_ID, buildNotification())
            }
        }

        return START_STICKY
    }

    private fun toggleWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (wakeLock == null) {
            // Tag "IDEku::WakeLock" untuk debugging di logcat
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "IDEku::TerminalWakeLock")
        }

        if (wakeLock?.isHeld == true) {
            releaseWakeLock()
        } else {
            wakeLock?.acquire()
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    private fun buildNotification(): Notification {
        // Setup Intent untuk tombol Exit
        val stopIntent = Intent(this, IDEkuService::class.java).apply { action = ACTION_STOP_SERVICE }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Setup Intent untuk tombol Wakelock
        val wakeIntent = Intent(this, IDEkuService::class.java).apply { action = ACTION_WAKELOCK_TOGGLE }
        val wakePendingIntent = PendingIntent.getService(this, 1, wakeIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val wakeLockStatus = if (wakeLock?.isHeld == true) "Release Wakelock" else "Activate Wakelock"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("IDEku Terminal is Active!")
            .setContentText("IDEku is running in the background...")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Menggunakan ikon sistem sesuai request Komandan
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_lock_idle_low_power, wakeLockStatus, wakePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Exit", stopPendingIntent)
            .build()
    }

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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }
}
