package com.thesozluk.app.network

import com.thesozluk.app.network.EksiSession.BASE_URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Connection
import org.jsoup.nodes.Document
import java.io.IOException

/** An entry in the trash (/cop): deleted by its author, still restorable. */
data class TrashEntry(
    val id: String,
    val topicTitle: String,
    /** e.g. "geçen ay" */
    val deletedAgo: String,
    val content: String,
    val contentHtml: String,
    val date: String
)

data class TrashPage(val entries: List<TrashEntry>, val page: Int, val pageCount: Int, val total: String)

/** The site's relation lists on /takip-engellenmis. */
enum class RelationKind(val code: String, val label: String, val empty: String) {
    Followed("b", "takip edilen", "kimseyi takip etmiyorsun"),
    Blocked("m", "engellenen", "engellediğin kimse yok"),
    TopicsBlocked("i", "başlıkları engellenen", "başlıklarını engellediğin kimse yok"),
    Muted("u", "sessize alınan", "sessize aldığın kimse yok")
}

data class RelationUser(val id: String, val nick: String, val avatarUrl: String?, val isVerified: Boolean)

data class RelationPage(val users: List<RelationUser>, val isLast: Boolean)

/**
 * The account's own lists from the site: the trash with restore / delete / empty, and the
 * follow and block lists with removing. Same requests as the site's pages make.
 */
object AccountLists {

    private fun request(url: String): Connection =
        EksiNetworkDataSource.newSessionRequest(url)
            .timeout(15000)
            .followRedirects(true)
            .ignoreHttpErrors(true)
            .ignoreContentType(true)
            .referrer("$BASE_URL/")

    // ------------------------------------------------------------------------------------- çöp

    suspend fun fetchTrash(page: Int): TrashPage = withContext(Dispatchers.IO) {
        val response = request("$BASE_URL/cop?p=$page").execute()
        if (response.statusCode() != 200) throw IOException("çöp kutusu açılamadı (HTTP ${response.statusCode()})")
        parseTrash(response.parse(), page)
    }

    private fun parseTrash(document: Document, page: Int): TrashPage {
        val entries = document.select("#content-body article").mapNotNull { article ->
            val restore = article.selectFirst("a[href*=/cop/canlandir]")?.attr("href") ?: return@mapNotNull null
            val id = Regex("id=(\\d+)").find(restore)?.groupValues?.get(1) ?: return@mapNotNull null
            val heading = article.selectFirst("h2")
            val content = article.selectFirst("p.content")
            TrashEntry(
                id = id,
                topicTitle = heading?.selectFirst("a")?.text()?.trim().orEmpty(),
                deletedAgo = heading?.selectFirst("time")?.text()?.trim().orEmpty(),
                content = content?.wholeText()?.trim().orEmpty(),
                contentHtml = content?.html().orEmpty(),
                date = article.selectFirst("footer")?.ownText()?.trim().orEmpty()
            )
        }
        val pager = document.selectFirst(".pager")
        val pageCount = pager?.attr("data-pagecount")?.toIntOrNull() ?: 1
        val total = document.select("#content-body p").firstOrNull { it.text().contains("çöpte") }?.text()?.trim().orEmpty()
        return TrashPage(entries, page, pageCount, total)
    }

    /**
     * Brings an entry back ("canlandır") or deletes it for good ("sil"). The site's links are
     * plain addresses behind a confirm box; whether it worked is checked on the trash itself.
     */
    private suspend fun trashAction(action: String, id: String): String? = withContext(Dispatchers.IO) {
        try {
            val response = request("$BASE_URL/cop/$action?id=$id").execute()
            if (response.statusCode() !in 200..399) return@withContext "işlem başarısız (HTTP ${response.statusCode()})"
            if (stillInTrash(id)) "işlem başarısız" else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.message ?: "işlem başarısız"
        }
    }

    private fun stillInTrash(id: String): Boolean {
        var page = 1
        while (true) {
            val response = request("$BASE_URL/cop?p=$page").execute()
            if (response.statusCode() != 200) return false
            val trash = parseTrash(response.parse(), page)
            if (trash.entries.any { it.id == id }) return true
            if (page >= trash.pageCount || page >= 20) return false
            page++
        }
    }

    suspend fun restore(id: String): String? = trashAction("canlandir", id)
    suspend fun deleteForever(id: String): String? = trashAction("sil", id)

    /** "çöpü boşalt": the page's form, with its token. */
    suspend fun emptyTrash(): String? = withContext(Dispatchers.IO) {
        try {
            val page = request("$BASE_URL/cop").execute().parse()
            val form = page.selectFirst("form[action*=/cop/bosalt]") ?: return@withContext "çöp zaten boş"
            val post = request("$BASE_URL/cop/bosalt")
                .method(Connection.Method.POST)
                .header("Origin", BASE_URL)
                .referrer("$BASE_URL/cop")
            form.select("input[type=hidden][name]").forEach { post.data(it.attr("name"), it.attr("value")) }
            val response = post.execute()
            if (response.statusCode() in 200..399) null else "işlem başarısız (HTTP ${response.statusCode()})"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.message ?: "işlem başarısız"
        }
    }

    // --------------------------------------------------------------------- takip / engellenmiş

    suspend fun fetchRelations(kind: RelationKind, page: Int): RelationPage = withContext(Dispatchers.IO) {
        val response = request("$BASE_URL/relation-list?relationType=${kind.code}&pageIndex=$page")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Accept", "application/json, text/javascript, */*; q=0.01")
            .referrer("$BASE_URL/takip-engellenmis")
            .execute()
        if (response.statusCode() != 200) throw IOException("liste alınamadı (HTTP ${response.statusCode()})")
        val relations = JSONObject(response.body()).getJSONObject("Relations")
        val items = relations.optJSONArray("Items")
        val users = (0 until (items?.length() ?: 0)).mapNotNull { i ->
            val item = items!!.getJSONObject(i)
            val nick = item.optJSONObject("Nick")?.optString("Value").orEmpty()
            if (nick.isBlank()) return@mapNotNull null
            RelationUser(
                id = item.optLong("Id").toString(),
                nick = nick,
                avatarUrl = item.optString("Picture").takeIf { it.startsWith("http") },
                isVerified = item.optBoolean("IsVerified")
            )
        }
        RelationPage(users, isLast = relations.optBoolean("IsLast", true) || users.isEmpty())
    }

    /** "kaldır": unfollow, unblock, show their topics again or unmute. */
    suspend fun removeRelation(kind: RelationKind, userId: String): String? =
        EksiNetworkDataSource.postRelation("/userrelation/removerelation/$userId?r=${kind.code}")
}
