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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
object SiteStatusCheck {
    // The worker and the live service may both check; one at a time, so a light isn't news twice
    private val lock = Mutex()

    /** One check; false when the site couldn't be read (try again later). */
    suspend fun run(context: Context): Boolean = lock.withLock { check(context.applicationContext) }

    private suspend fun check(context: Context): Boolean {
        EksiSession.init(context)
        if (!EksiSession.isLoggedIn.value) return true
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val status = EksiNetworkDataSource.fetchSiteStatus()
        // When it last ran and how it went, for ayarlar → bildirimler
        prefs.edit()
            .putLong(KEY_LAST_CHECK, System.currentTimeMillis())
            .putString(KEY_LAST_PROBLEM, if (status == null) EksiNetworkDataSource.siteStatusProblem ?: "siteye ulaşılamadı" else null)
            .apply()
        status ?: return false

        val messageCounts = notifyMessages(context, prefs, status.hasMessages)
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
                    context, id, title, "$count yeni entry", Notifier.OPEN_EVENTS,
                    topicPath = path
                )
            }
        }
        prefs.edit()
            .putBoolean(KEY_MESSAGES, status.hasMessages)
            .putString(KEY_EVENT_COUNTS, current.toString())
            .apply {
                if (messageCounts != null) putString(KEY_MESSAGE_COUNTS, messageCounts.toString())
            }
            .apply()
        return true
    }

    /**
     * The message light stays on until the box is read, so a light that is merely on says nothing
     * about a second message. While it is on, the first page of the box is compared with the last
     * check: a conversation that is new there or has more messages is news. Returns the counts to
     * keep (none once the light is off), or null when the box couldn't be read (then only a light
     * that just turned on notifies).
     */
    private suspend fun notifyMessages(context: Context, prefs: android.content.SharedPreferences, lightOn: Boolean): JSONObject? {
        val lightWasOn = prefs.getBoolean(KEY_MESSAGES, false)
        // Read box: start over, so the user's own replies meanwhile don't count as news later
        if (!lightOn) return JSONObject()
        val threads = try {
            EksiNetworkDataSource.fetchMessageBox(archive = false, page = 1).threads
        } catch (e: Exception) {
            null
        }
        if (threads == null) {
            if (!lightWasOn) {
                Notifier.show(context, ID_MESSAGES, "yeni mesajın var", "okumak için dokun", Notifier.OPEN_MESSAGES)
            }
            return null
        }
        val previous = runCatching { JSONObject(prefs.getString(KEY_MESSAGE_COUNTS, "{}").orEmpty()) }.getOrDefault(JSONObject())
        val current = JSONObject()
        threads.forEach { current.put(it.id, it.messageCount) }
        val fresh = threads.filter { !previous.has(it.id) || it.messageCount > previous.optInt(it.id) }
        when {
            // The light just turned on: the box lists the latest conversation first
            !lightWasOn || previous.length() == 0 -> if (!lightWasOn) {
                val thread = threads.firstOrNull()
                Notifier.show(
                    context, ID_MESSAGES, thread?.nick?.ifBlank { null } ?: "yeni mesajın var",
                    thread?.preview?.ifBlank { null } ?: "okumak için dokun", Notifier.OPEN_MESSAGES
                )
            }
            fresh.size == 1 -> {
                val thread = fresh.first()
                Notifier.show(
                    context, ID_MESSAGES, thread.nick.ifBlank { "yeni mesajın var" },
                    thread.preview.ifBlank { "okumak için dokun" }, Notifier.OPEN_MESSAGES
                )
            }
            fresh.size > 1 -> Notifier.show(
                context, ID_MESSAGES, "${fresh.size} konuşmada yeni mesaj",
                fresh.joinToString(", ") { it.nick }, Notifier.OPEN_MESSAGES
            )
        }
        return current
    }

    private const val PREFS = "site_status"
    private const val KEY_LAST_CHECK = "last_check"
    private const val KEY_LAST_PROBLEM = "last_problem"

    /** The last background check: when (0 if never) and what went wrong (null if nothing). */
    fun lastCheck(context: Context): Pair<Long, String?> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_CHECK, 0L) to prefs.getString(KEY_LAST_PROBLEM, null)
    }
    private const val KEY_MESSAGES = "messages"
    private const val KEY_MESSAGE_COUNTS = "message_counts"
    private const val KEY_EVENT_COUNTS = "event_counts"
    private const val ID_MESSAGES = 1
    private const val ID_EVENTS = 2
}

/** The periodic check WorkManager runs, every 15 minutes at best. */
class SiteStatusWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    // A failed check waits for the next round: a retry would back off (up to hours) and push the
    // following checks back with it
    override suspend fun doWork(): Result {
        SiteStatusCheck.run(applicationContext)
        return Result.success()
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
        // "anlık bildirimler" adds the live service on top; the 15-minute check stays as a fallback
        LiveCheckService.sync(context, enabled && LiveCheckService.wanted(context))
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
            // The palette chosen in settings (wallpaper colors aren't known in the background)
            .setColor(notificationColor(context))
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

    private fun notificationColor(context: Context): Int {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val seed = com.thesozluk.app.settings.SeedColor.entries
            .getOrElse(prefs.getInt("seed", com.thesozluk.app.settings.SeedColor.Brand.ordinal)) { com.thesozluk.app.settings.SeedColor.Brand }
        return seed.argb.toInt()
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, "mesaj ve olay bildirimleri", NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
