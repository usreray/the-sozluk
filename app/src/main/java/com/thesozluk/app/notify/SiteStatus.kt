package com.thesozluk.app.notify

import org.json.JSONObject
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.thesozluk.app.MainActivity
import com.thesozluk.app.R
import com.thesozluk.app.network.EksiNetworkDataSource
import com.thesozluk.app.network.EksiSession
import java.util.concurrent.TimeUnit

/** The site's header lights (GET /top/led): unread messages and new entries in followed topics. */
data class SiteStatus(val hasMessages: Boolean = false, val hasEvents: Boolean = false)

/** In-app copy of the lights, for badges on the home screen. */
object SiteStatusStore {
    private val _status = mutableStateOf(SiteStatus())
    val status: State<SiteStatus> = _status

    suspend fun refresh() {
        if (!EksiSession.isLoggedIn.value) {
            _status.value = SiteStatus()
            return
        }
        EksiNetworkDataSource.fetchSiteStatus()?.let { _status.value = it }
    }
}

/**
 * Checks the lights in the background. Messages notify when their light turns on. The olay light
 * stays on until the list is opened, so for followed topics the olay list itself is compared with
 * the last check: a topic that is new there, or has more new entries, is news.
 */
class SiteStatusWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        EksiSession.init(applicationContext)
        if (!EksiSession.isLoggedIn.value) return Result.success()
        val status = EksiNetworkDataSource.fetchSiteStatus() ?: return Result.retry()

        // Only a light that just turned on is news; one already notified stays quiet
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (status.hasMessages && !prefs.getBoolean(KEY_MESSAGES, false)) {
            Notifier.show(applicationContext, ID_MESSAGES, "yeni mesajın var", "okumak için dokun", Notifier.OPEN_MESSAGES)
        }
        val previous = runCatching { JSONObject(prefs.getString(KEY_EVENT_COUNTS, "{}").orEmpty()) }.getOrDefault(JSONObject())
        var current = JSONObject()
        val fresh = mutableListOf<Triple<String, Int, String>>()
        if (status.hasEvents) {
            val topics = try {
                EksiNetworkDataSource.fetchTopics(1, "basliklar/olay")
            } catch (e: Exception) {
                null
            }
            if (topics == null) {
                current = previous
            } else {
                topics.forEach { topic ->
                    // Being listed in olay means at least one new entry, even without a count
                    val count = maxOf(topic.commentCount, 1)
                    current.put(topic.title, count)
                    val before = previous.optInt(topic.title, 0)
                    if (count > before) fresh += Triple(topic.title, count - before, topic.url)
                }
            }
        }
        if (fresh.isNotEmpty()) {
            fresh.forEach { (title, count, path) ->
                val id = 1000 + (path.hashCode() and 0x3fffffff)
                Notifier.show(
                    applicationContext, id, title, "$count yeni entry", Notifier.OPEN_EVENTS,
                    topicPath = path
                )
            }
        }
        prefs.edit()
            .putBoolean(KEY_MESSAGES, status.hasMessages)
            .putString(KEY_EVENT_COUNTS, current.toString())
            .apply()
        return Result.success()
    }

    private companion object {
        const val PREFS = "site_status"
        const val KEY_MESSAGES = "messages"
        const val KEY_EVENT_COUNTS = "event_counts"
        const val ID_MESSAGES = 1
        const val ID_EVENTS = 2
    }
}

object Notifier {
    const val EXTRA_OPEN = "open"
    const val EXTRA_TOPIC_PATH = "topic_path"
    const val OPEN_MESSAGES = "messages"
    const val OPEN_EVENTS = "olay"
    private const val CHANNEL = "site_status"
    private const val WORK = "site_status_check"

    /** Starts or stops the periodic check (every 15 minutes, the shortest WorkManager allows). */
    fun schedule(context: Context, enabled: Boolean) {
        val work = WorkManager.getInstance(context)
        if (!enabled) {
            work.cancelUniqueWork(WORK)
            return
        }
        createChannel(context)
        val request = PeriodicWorkRequestBuilder<SiteStatusWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        work.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(context: Context, id: Int, title: String, text: String, open: String, topicPath: String? = null) {
        if (!canNotify(context)) return
        createChannel(context)
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_OPEN, open)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        topicPath?.let { intent.putExtra(EXTRA_TOPIC_PATH, it).putExtra("topic_title", title) }
        val pending = PendingIntent.getActivity(
            context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // Permission revoked between the check and the call
        }
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, "mesaj ve olay bildirimleri", NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
