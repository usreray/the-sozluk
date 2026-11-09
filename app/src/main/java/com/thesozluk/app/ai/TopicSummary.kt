package com.thesozluk.app.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.thesozluk.app.model.Entry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** How "başlığı özetle" works, chosen under ayarlar → yapay zeka. */
/** How long a summary should be, chosen under ayarlar → yapay zeka; [topic] and [entry] tell the model. */
enum class SummaryLength(val label: String, val topic: String, val entry: String) {
    VeryShort(
        "çok kısa",
        "Tek bir kısa cümleyle özetle; madde kullanma.",
        "Tek bir kısa cümleyle özetle; madde kullanma."
    ),
    Short(
        "kısa",
        "3 madde ile özetle. Her madde '• ' ile başlasın ve kısa tek cümle olsun.",
        "2 ya da 3 cümleyle özetle; madde kullanma."
    ),
    Medium(
        "orta",
        "3 ile 6 madde arasında özetle. Her madde '• ' ile başlasın ve tek cümle olsun.",
        "3 ya da 4 madde ile özetle. Her madde '• ' ile başlasın ve tek cümle olsun."
    ),
    Long(
        "ayrıntılı",
        "6 ile 10 madde arasında, görüşleri ve aralarındaki farkları ayrıntılı anlatarak özetle. Her madde '• ' ile başlasın.",
        "5 ile 8 madde arasında, anlatılanları sırasıyla ayrıntılı özetle. Her madde '• ' ile başlasın."
    )
}

/** A model to pick for an engine: its id, a short name and what it is good for. */
data class AiModel(val id: String, val label: String, val note: String)

enum class AiEngine(val label: String, val short: String) {
    Off("kapalı", "kapalı"),
    Gemini("gemini (kendi anahtarın)", "gemini"),
    Claude("claude (kendi anahtarın)", "claude"),
    OpenAi("openai (kendi anahtarın)", "openai"),
    /** Any service that speaks OpenAI's chat completions: NVIDIA, OpenRouter, Groq, a local server... */
    Custom("diğer (openai uyumlu)", "diğer")
}

/**
 * Summarizes topic pages and entries with the user's own key for Gemini, Claude, OpenAI or any
 * OpenAI-compatible service. The keys live in their own preferences, outside the app's backups.
 */
object TopicSummary {
    private const val PREFS = "ai_settings"
    // Names the providers keep pointing at their newest model, so an old app still gets a current one
    private const val GEMINI_LATEST = "gemini-flash-latest"
    private const val OPENAI_LATEST = "chat-latest"

    /** The models to pick from, the first one the default. */
    val models: Map<AiEngine, List<AiModel>> = mapOf(
        AiEngine.Gemini to listOf(
            AiModel(GEMINI_LATEST, "en yeni", "google'ın en yeni flash modeli; yenisi çıkınca kendiliğinden ona geçer"),
            AiModel("gemini-3.8-flash", "3.8 flash", "en iyi özetler"),
            AiModel("gemini-3.5-flash-lite", "3.5 lite", "daha hızlı ve ucuz, özetler daha basit")
        ),
        AiEngine.Claude to listOf(
            AiModel("claude-opus-5-5", "opus 5.5", "en iyi özetler, en pahalısı"),
            AiModel("claude-sonnet-5-5", "sonnet 5.5", "iyi özetler, yarı fiyatına"),
            AiModel("claude-haiku-5-5", "haiku 5.5", "en hızlı ve en ucuzu, özetler daha basit")
        ),
        AiEngine.OpenAi to listOf(
            AiModel(OPENAI_LATEST, "en yeni", "chatgpt'deki en yeni model; yenisi çıkınca kendiliğinden ona geçer"),
            AiModel("gpt-6.1-sol", "6.1 sol", "en iyiye yakın özetler, makul fiyata"),
            AiModel("gpt-6-astra", "6 astra", "en iyi özetler, en pahalısı"),
            AiModel("gpt-6-luna", "6 luna", "en hızlı ve en ucuzu, özetler daha basit")
        )
    )

    /** "diğer": the service's address (up to /v1) and the model's name, typed by the user. */
    fun customUrl(): String = prefs.getString("custom_url", null).orEmpty()
    fun customModel(): String = prefs.getString("custom_model", null).orEmpty()
    fun setCustomUrl(url: String) = prefs.edit().putString("custom_url", url.trim()).apply()
    fun setCustomModel(model: String) = prefs.edit().putString("custom_model", model.trim()).apply()

    private lateinit var prefs: SharedPreferences
    private val _engine = mutableStateOf(AiEngine.Off)
    val engine: State<AiEngine> = _engine

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _engine.value = AiEngine.entries.firstOrNull { it.name == prefs.getString("engine", null) } ?: AiEngine.Off
        _length.value = SummaryLength.entries.firstOrNull { it.name == prefs.getString("length", null) } ?: SummaryLength.Medium
    }

    fun setEngine(engine: AiEngine) {
        _engine.value = engine
        prefs.edit().putString("engine", engine.name).apply()
    }

    private val _modelChanges = mutableStateOf(0)

    /** The model chosen for [engine] (the default when none is). */
    fun model(engine: AiEngine): AiModel {
        _modelChanges.value // read, so the settings page redraws when it changes
        val options = models[engine].orEmpty()
        val id = prefs.getString("model_${engine.name}", null)
        return options.firstOrNull { it.id == id } ?: options.first()
    }

    fun setModel(engine: AiEngine, model: AiModel) {
        prefs.edit().putString("model_${engine.name}", model.id).apply()
        _modelChanges.value++
    }

    private val _length = mutableStateOf(SummaryLength.Medium)
    val length: State<SummaryLength> = _length

    fun setLength(length: SummaryLength) {
        _length.value = length
        prefs.edit().putString("length", length.name).apply()
    }

    fun key(engine: AiEngine): String = prefs.getString("key_${engine.name}", null).orEmpty()

    fun setKey(engine: AiEngine, key: String) {
        prefs.edit().putString("key_${engine.name}", key.trim()).apply()
    }

    /** A topic page's entries summarized in Turkish; [progress] says what it is doing meanwhile. */
    suspend fun summarize(title: String, entries: List<Entry>, progress: (String) -> Unit): String {
        val instructions = TOPIC_INSTRUCTIONS + " " + length.value.topic + " " + RULES
        return ask(instructions, "Başlık: $title\n\nEntry'ler:\n" + entryText(entries, API_LIMIT), progress)
    }

    /** One entry summarized in Turkish; [title] is its topic when known. */
    suspend fun summarizeEntry(title: String, entry: Entry, progress: (String) -> Unit): String {
        val instructions = ENTRY_INSTRUCTIONS + " " + length.value.entry + " " + RULES
        val text = entry.content.trim().take(API_LIMIT)
        return ask(instructions, (if (title.isNotBlank()) "Başlık: $title\n\n" else "") + "Entry:\n" + text, progress)
    }

    /**
     * [text] with its spelling and punctuation fixed and nothing else touched: the words, the tone,
     * ekşi's all-lowercase style and its markup stay as written. Returns the text unchanged when
     * there is nothing to fix.
     */
    suspend fun proofread(text: String, progress: (String) -> Unit): String =
        ask(PROOFREAD_INSTRUCTIONS, text.take(API_LIMIT), progress)

    private const val PROOFREAD_INSTRUCTIONS =
        "Sana ekşi sözlük için yazılmış bir metin veriliyor. Yazım (imla) ve noktalamasını düzelt: yanlış yazılmış " +
            "kelimeler, ayrı ya da bitişik yazılması gerekenler (de/da, ki, mi), Türkçe karakter eksikleri ve " +
            "noktalama. Noktalama hiç yoksa ya da eksikse gereken yerlere ekle: cümle sonlarına nokta, soru " +
            "cümlelerine soru işareti, gereken yerlere virgül; yanlış kullanılmışsa düzelt. Kelime seçimine, " +
            "cümle yapısına, üsluba ve argoya dokunma. Küçük harfleri büyütme, cümle başlarını da büyütme; ekşi " +
            "sözlük'te her şey küçük harfle yazılır. (bkz: ...), `...`, `:...`, [link] ve --- `spoiler` --- gibi " +
            "işaretleri ve satır sonlarını aynen koru. Düzeltilecek bir şey yoksa metni aynen geri ver. Sadece " +
            "düzeltilmiş metni yaz; açıklama, tırnak ya da başka bir şey ekleme."

    private suspend fun ask(instructions: String, prompt: String, progress: (String) -> Unit): String {
        val engine = engine.value
        require(engine != AiEngine.Off) { "yapay zeka kapalı; ayarlar → yapay zeka" }
        if (engine == AiEngine.Custom) {
            // A local server may need no key, but it needs an address and a model
            if (customUrl().isBlank() || customModel().isBlank()) throw IOException("önce ayarlar → yapay zeka'dan adresi ve modeli gir")
        } else if (key(engine).isBlank()) {
            throw IOException("önce ayarlar → yapay zeka'dan anahtarını gir")
        }
        return when (engine) {
            AiEngine.Gemini -> gemini(instructions, prompt, progress)
            AiEngine.Claude -> claude(instructions, prompt, progress)
            AiEngine.OpenAi -> openAi(instructions, prompt, progress)
            AiEngine.Custom -> compatible(instructions, prompt, progress)
            AiEngine.Off -> ""
        }
    }

    // ---------------------------------------------------------------------------------------
    // the request text

    private const val API_LIMIT = 120_000

    private const val TOPIC_INSTRUCTIONS =
        "Sana ekşi sözlük'teki bir başlığın bir sayfasındaki entry'ler veriliyor. Bunları Türkçe özetle: " +
            "başlıkta neler konuşulduğunu, öne çıkan görüşleri, karşı görüşleri ve varsa ortak şakaları anlat."

    private const val ENTRY_INSTRUCTIONS =
        "Sana ekşi sözlük'teki bir entry veriliyor. Onu Türkçe özetle: yazarın ne anlattığını, vardığı sonucu " +
            "ve varsa önerilerini anlat."

    private const val RULES =
        "Sadece verilen metinde yazanlara dayan, bilgi ekleme. Yazarların adlarını verme. Özetten başka bir şey yazma."

    /** The entries as "• text" lines, the newest page order kept, cut at [limit] characters. */
    private fun entryText(entries: List<Entry>, limit: Int): String = buildString {
        for (entry in entries) {
            val line = entry.content.replace(Regex("\\s+"), " ").trim()
            if (line.isEmpty()) continue
            if (length + line.length > limit) break
            append("• ").append(line).append('\n')
        }
    }

    // ---------------------------------------------------------------------------------------
    // Gemini, with the user's key

    private suspend fun gemini(
        instructions: String,
        prompt: String,
        progress: (String) -> Unit,
        modelId: String = model(AiEngine.Gemini).id,
        lightThinking: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        progress("gemini'ye soruluyor")
        val body = JSONObject()
            .put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", instructions))))
            .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
        // The newer models think at length before answering by default; a summary or a spelling
        // check needs little of it, and thinking less keeps them from taking minutes
        if (lightThinking) {
            body.put("generationConfig", JSONObject().put("thinkingConfig", JSONObject().put("thinkingLevel", "low")))
        }
        // A busy model (503) usually answers a few seconds later
        var attempt = 0
        while (true) {
            val connection = URL("https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent")
                .openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 15_000
                connection.readTimeout = 90_000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("x-goog-api-key", key(AiEngine.Gemini))
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
                val code = try {
                    connection.responseCode
                } catch (e: java.net.SocketTimeoutException) {
                    throw IOException("gemini çok uzun sürdü; daha hafif bir model dene", e)
                }
                // The chosen model was retired: the newest one instead
                if (code == 404 && modelId != GEMINI_LATEST) {
                    return@withContext gemini(instructions, prompt, progress, GEMINI_LATEST, lightThinking)
                }
                // A model that takes no thinking setting: ask again without it
                if (code == 400 && lightThinking) {
                    val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    if (error.contains("thinking", ignoreCase = true)) {
                        return@withContext gemini(instructions, prompt, progress, modelId, lightThinking = false)
                    }
                }
                if ((code == 503 || code == 500) && attempt < 2) {
                    attempt++
                    progress("gemini yoğun, tekrar deneniyor")
                    kotlinx.coroutines.delay(3_000L * attempt)
                    continue
                }
                if (code != 200) {
                    throw IOException(
                        when (code) {
                            400, 401, 403 -> "gemini anahtarı geçersiz ya da yetkisiz"
                            429 -> "gemini kullanım sınırı doldu; biraz sonra tekrar dene"
                            500, 503 -> "gemini şu an çok yoğun; biraz sonra tekrar dene ya da daha hafif bir model seç"
                            else -> "gemini yanıt vermedi (HTTP $code)"
                        }
                    )
                }
                val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                val parts = json.optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                // Thought summaries (if any) come marked "thought"; only the answer counts
                val text = (0 until (parts?.length() ?: 0))
                    .mapNotNull { parts!!.optJSONObject(it) }
                    .filter { !it.optBoolean("thought") }
                    .joinToString("") { it.optString("text") }
                return@withContext text.trim().ifEmpty { throw IOException("gemini bu metni işlemedi") }
            } finally {
                connection.disconnect()
            }
        }
        @Suppress("UNREACHABLE_CODE")
        ""
    }

    // ---------------------------------------------------------------------------------------
    // Claude, with the user's key: the Messages API over plain HTTPS (the SDK would add tens of
    // megabytes to the app for this one request)

    private suspend fun claude(instructions: String, prompt: String, progress: (String) -> Unit): String = withContext(Dispatchers.IO) {
        progress("claude'a soruluyor")
        val body = JSONObject()
            .put("model", model(AiEngine.Claude).id)
            .put("max_tokens", 16000)
            .put("system", instructions)
            .put("output_config", JSONObject().put("effort", "medium"))
            // A declined request is retried on Anthropic's recommended model instead of failing
            .put("fallbacks", "default")
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
        val connection = URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 180_000
            connection.doOutput = true
            connection.setRequestProperty("content-type", "application/json")
            connection.setRequestProperty("x-api-key", key(AiEngine.Claude))
            connection.setRequestProperty("anthropic-version", "2023-06-01")
            connection.setRequestProperty("anthropic-beta", "server-side-fallback-2026-07-01")
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = connection.responseCode
            if (code != 200) {
                throw IOException(
                    when (code) {
                        401, 403 -> "claude anahtarı geçersiz ya da yetkisiz"
                        402 -> "claude hesabında bakiye yok"
                        429 -> "claude kullanım sınırı doldu; biraz sonra tekrar dene"
                        529 -> "claude şu an çok yoğun; biraz sonra tekrar dene"
                        else -> "claude yanıt vermedi (HTTP $code)"
                    }
                )
            }
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            if (json.optString("stop_reason") == "refusal") throw IOException("claude bu başlığı özetlemeyi reddetti")
            val content = json.optJSONArray("content")
            val text = (0 until (content?.length() ?: 0))
                .mapNotNull { content!!.optJSONObject(it) }
                .filter { it.optString("type") == "text" }
                .joinToString("") { it.optString("text") }
            text.trim().ifEmpty { throw IOException("claude bu başlığı özetlemedi") }
        } finally {
            connection.disconnect()
        }
    }

    // ---------------------------------------------------------------------------------------
    // OpenAI, with the user's key: the Responses API

    private suspend fun openAi(
        instructions: String,
        prompt: String,
        progress: (String) -> Unit,
        modelId: String = model(AiEngine.OpenAi).id
    ): String = withContext(Dispatchers.IO) {
        progress("openai'ye soruluyor")
        val body = JSONObject()
            .put("model", modelId)
            .put("instructions", instructions)
            .put("input", prompt)
        val json = try {
            post("https://api.openai.com/v1/responses", body, "openai", key(AiEngine.OpenAi))
        } catch (e: ModelNotFound) {
            // The chosen model was retired: the newest one instead
            if (modelId == OPENAI_LATEST) throw e
            return@withContext openAi(instructions, prompt, progress, OPENAI_LATEST)
        }
        // The answer is the output_text parts of the message items
        val output = json.optJSONArray("output")
        val text = buildString {
            for (i in 0 until (output?.length() ?: 0)) {
                val item = output!!.optJSONObject(i) ?: continue
                if (item.optString("type") != "message") continue
                val content = item.optJSONArray("content") ?: continue
                for (j in 0 until content.length()) {
                    val part = content.optJSONObject(j) ?: continue
                    if (part.optString("type") == "output_text") append(part.optString("text"))
                }
            }
        }
        text.trim().ifEmpty { throw IOException("openai bu metni özetlemedi") }
    }

    // ---------------------------------------------------------------------------------------
    // "diğer": anything that speaks OpenAI's chat completions (NVIDIA, OpenRouter, Groq...)

    private suspend fun compatible(instructions: String, prompt: String, progress: (String) -> Unit): String = withContext(Dispatchers.IO) {
        progress("yapay zekaya soruluyor")
        val body = JSONObject()
            .put("model", customModel())
            .put(
                "messages", JSONArray()
                    .put(JSONObject().put("role", "system").put("content", instructions))
                    .put(JSONObject().put("role", "user").put("content", prompt))
            )
        val url = customUrl().trimEnd('/').let { if (it.endsWith("/chat/completions")) it else "$it/chat/completions" }
        val json = post(url, body, "servis", key(AiEngine.Custom))
        val text = json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty()
        // Some reasoning models write their thinking in <think> tags before the answer
        text.replace(Regex("(?s)<think>.*?</think>"), "").trim().ifEmpty { throw IOException("servis bu metni özetlemedi") }
    }

    /** POSTs [body] with a bearer [key] (if any) and returns the JSON answer, or a readable error. */
    private fun post(url: String, body: JSONObject, name: String, key: String): JSONObject {
        val connection = try {
            URL(url).openConnection() as HttpURLConnection
        } catch (e: Exception) {
            throw IOException("adres geçersiz: $url", e)
        }
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 180_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            if (key.isNotBlank()) connection.setRequestProperty("Authorization", "Bearer $key")
            connection.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = connection.responseCode
            if (code == 404) throw ModelNotFound("$name bu modeli ya da adresi bulamadı")
            if (code != 200) {
                throw IOException(
                    when (code) {
                        401, 403 -> "$name anahtarı geçersiz ya da yetkisiz"
                        402 -> "$name hesabında bakiye yok"
                        429 -> "$name kullanım sınırı doldu ya da bakiye yok; biraz sonra tekrar dene"
                        else -> "$name yanıt vermedi (HTTP $code)"
                    }
                )
            }
            return JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }
}

/** The service doesn't know the model (or the address): a retired model, or a typo. */
private class ModelNotFound(message: String) : IOException(message)
