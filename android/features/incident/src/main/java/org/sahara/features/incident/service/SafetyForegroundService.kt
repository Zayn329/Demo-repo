package org.sahara.features.incident.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import org.sahara.core.domain.models.IncidentState
import org.sahara.features.incident.statemachine.IncidentStateMachine

class SafetyForegroundService : Service() {

    private val binder = LocalBinder()
    var stateMachine: IncidentStateMachine? = null

    inner class LocalBinder : Binder() {
        fun getService(): SafetyForegroundService = this@SafetyForegroundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("Sahara Safety Monitoring Active", "Monitoring for potential distress signals...")
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    fun updateNotificationForState(state: IncidentState) {
        val title: String
        val content: String

        when (state) {
            IncidentState.MONITORING -> {
                title = "Sahara Monitoring Active"
                content = "Background safety detection is active."
            }
            IncidentState.CANDIDATE_INCIDENT, IncidentState.PENDING_CONFIRMATION -> {
                title = "Possible Distress Detected"
                content = "Evaluating confirmation rules..."
            }
            IncidentState.ACTIVE_INCIDENT -> {
                title = "EMERGENCY: Incident Active"
                content = "Evidence protection and alert dispatch in progress."
            }
            IncidentState.CANCELLED -> {
                title = "Incident Cancelled"
                content = "Captured evidence is safely preserved locally."
            }
            IncidentState.SEALED -> {
                title = "Incident Sealed"
                content = "Evidence package sealed with cryptographic integrity."
            }
            else -> {
                title = "Sahara Safety Companion"
                content = "Status: $state"
            }
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(title, content))
    }

    private fun createNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sahara Safety Monitoring",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Persistent notification for Sahara offline background distress detection"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "sahara_safety_channel"
        const val NOTIFICATION_ID = 1001
    }
}
