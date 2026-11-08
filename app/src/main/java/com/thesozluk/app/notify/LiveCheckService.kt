package com.thesozluk.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.thesozluk.app.MainActivity
import com.thesozluk.app.R
import com.thesozluk.app.network.EksiSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * "anlık bildirimler": a foreground service that checks for new messages and olay entries every
 * minute instead of every 15. ekşi has no push for other apps, so the only way to hear sooner is to
 * ask more often, which is why it stays in the notification shade and costs battery. While the
 * phone sleeps Android still holds the timer back; it catches up as soon as the phone wakes.
 */
class LiveCheckService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            ServiceCompat.startForeground(
                this, ONGOING_ID, ongoingNotification(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
            )
        } catch (e: Exception) {
            // Not allowed to run in the foreground right now; the 15-minute check carries on
            stopSelf()
            return START_NOT_STICKY
        }
        if (loop?.isActive != true) loop = scope.launch { checkEveryMinute() }
        return START_STICKY
    }

    private suspend fun checkEveryMinute() {
        val power = getSystemService(PowerManager::class.java)
        while (scope.isActive) {
            EksiSession.init(applicationContext)
            if (!EksiSession.isLoggedIn.value || !wanted(this)) {
                stopSelf()
                return
            }
            // Awake only for the check itself, not between checks
            val wake = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "thesozluk:check")
            wake.acquire(45_000L)
            try {
                SiteStatusCheck.run(applicationContext)
            } finally {
                if (wake.isHeld) wake.release()
            }
            delay(INTERVAL_MS)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun ongoingNotification(): android.app.Notification {
        val channel = NotificationChannel(CHANNEL, "anlık bildirim servisi", NotificationManager.IMPORTANCE_MIN).apply {
            description = "anlık bildirimler açıkken duran kalıcı bildirim"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val open = PendingIntent.getActivity(
            this, ONGOING_ID, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("anlık bildirimler açık")
            .setContentText("yeni mesaj ve entry'lere dakikada bir bakılıyor")
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        private const val CHANNEL = "live_check"
        private const val ONGOING_ID = 3
        private const val INTERVAL_MS = 60_000L
        const val PREF = "instantNotifications"

        /** Whether "anlık bildirimler" is on (read straight from the settings, for the boot receiver too). */
        fun wanted(context: Context): Boolean =
            context.getSharedPreferences("app_settings", Context.MODE_PRIVATE).getBoolean(PREF, false)

        /** Starts or stops the service. */
        fun sync(context: Context, on: Boolean) {
            val intent = Intent(context, LiveCheckService::class.java)
            if (!on) {
                context.stopService(intent)
                return
            }
            try {
                context.startForegroundService(intent)
            } catch (e: Exception) {
                // Started from the background where Android doesn't allow it; the next open retries
            }
        }
    }
}

/** Brings "anlık bildirimler" back after a restart of the phone or an update of the app. */
class LiveCheckBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        EksiSession.init(context.applicationContext)
        val notifications = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE).getBoolean("notifications", true)
        if (notifications && EksiSession.isLoggedIn.value && LiveCheckService.wanted(context) && Notifier.canNotify(context)) {
            LiveCheckService.sync(context, true)
        }
    }
}
