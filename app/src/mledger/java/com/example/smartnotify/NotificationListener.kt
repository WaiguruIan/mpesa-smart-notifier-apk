package com.example.smartnotify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationListener : NotificationListenerService() {

    private val CHANNEL_ID = "mpesa_silent_channel"
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        createNotificationChannel()
        Log.e("SMARTNOTIFY", "!!! LEDGER SERVICE CONNECTED !!!")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val senderPackage = sbn.packageName ?: "unknown"
        if (senderPackage == packageName || senderPackage.contains("ntfy")) return

        try {
            val extras = sbn.notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

            if (title.contains("MPESA", ignoreCase = true)) {
                cancelNotification(sbn.key)
                val fullMessage = if (bigText.isNotEmpty()) bigText else text
                val isForwardable = processMpesaMessage(fullMessage)
                showSilentNotification(title, fullMessage)
                
                // LEDGER VERSION: Always save and forward
                if (isForwardable) {
                    saveMessageToDb(fullMessage)
                }
            }
        } catch (e: Exception) {
            Log.e("SMARTNOTIFY", "Error: ${e.message}")
        }
    }

    private fun saveMessageToDb(messageContent: String) {
        serviceScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val mpesaMessage = MpesaMessage(
                rawBody = messageContent,
                timestamp = System.currentTimeMillis(),
                status = "QUEUED"
            )
            db.mpesaDao().insert(mpesaMessage)
            triggerWorker()
        }
    }

    private fun triggerWorker() {
        val workRequest = OneTimeWorkRequestBuilder<MpesaWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "MpesaForwardingWork",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun showSilentNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "M-PESA Ledger", NotificationManager.IMPORTANCE_LOW)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun processMpesaMessage(message: String): Boolean {
        val lower = message.lowercase()
        if (lower.contains("received") || lower.contains("sent") || lower.contains("paid")) {
            playSound(R.raw.mpesa_sound)
            return true
        }
        playSound(R.raw.balance)
        return false
    }

    private fun playSound(resId: Int) {
        MediaPlayer.create(this, resId)?.apply {
            setOnCompletionListener { it.release() }
            start()
        }
    }
}
