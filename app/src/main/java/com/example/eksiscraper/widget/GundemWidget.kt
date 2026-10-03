package com.example.eksiscraper.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.eksiscraper.MainActivity
import com.example.eksiscraper.R
import com.example.eksiscraper.model.Topic
import com.example.eksiscraper.network.EksiNetworkDataSource
import com.example.eksiscraper.network.EksiSession
import com.example.eksiscraper.settings.AppSettings

/**
 * Home screen widget: the gündem topics with their entry counts; a topic opens in the app,
 * the refresh icon reloads the list (it also refreshes on its own every half hour).
 */
class GundemWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        EksiSession.init(context)
        AppSettings.init(context)
        val topics = try {
            EksiNetworkDataSource.fetchTopics(1, "popular").filterNot { AppSettings.isBlocked(it.title) }.take(20)
        } catch (e: Exception) {
            null
        }
        provideContent { GlanceTheme { Content(context, topics) } }
    }

    @Composable
    private fun Content(context: Context, topics: List<Topic>?) {
        val colors = GlanceTheme.colors
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(colors.widgetBackground)
                .cornerRadius(24.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    "gündem",
                    style = TextStyle(color = colors.primary, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
                )
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_refresh),
                    contentDescription = "yenile",
                    modifier = GlanceModifier.size(28.dp).padding(4.dp).clickable(actionRunCallback<RefreshGundem>())
                )
            }
            Spacer(GlanceModifier.height(6.dp))
            if (topics.isNullOrEmpty()) {
                Text(
                    if (topics == null) "yüklenemedi; yenilemek için dokun" else "başlık yok",
                    style = TextStyle(color = colors.onSurfaceVariant, fontSize = 13.sp),
                    modifier = GlanceModifier.clickable(actionRunCallback<RefreshGundem>())
                )
            } else {
                LazyColumn {
                    items(topics) { topic ->
                        // Opens like an ekşi link from another app
                        val open = Intent(context, MainActivity::class.java)
                            .setAction(Intent.ACTION_VIEW)
                            .setData(Uri.parse(if (topic.url.startsWith("http")) topic.url else EksiSession.BASE_URL + topic.url))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 5.dp).clickable(actionStartActivity(open))
                        ) {
                            Text(
                                topic.title,
                                maxLines = 2,
                                style = TextStyle(color = colors.onSurface, fontSize = 14.sp),
                                modifier = GlanceModifier.defaultWeight()
                            )
                            if (topic.commentCount > 0) {
                                Text(
                                    topic.commentCount.toString(),
                                    style = TextStyle(color = colors.onSurfaceVariant, fontSize = 12.sp),
                                    modifier = GlanceModifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The refresh icon: fetch gündem again. */
class RefreshGundem : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        GundemWidget().update(context, glanceId)
    }
}

class GundemWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GundemWidget()
}
