package com.thesozluk.app.settings

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

/** One version's highlights for the "neler yeni" sheet. */
data class Release(val version: String, val items: List<Pair<String, String>>)

/**
 * "neler yeni": after an update the app opens once with what came in it. A fresh install skips
 * it (everything is new then); hakkında can show it again at any time.
 */
object WhatsNew {
    private const val PREFS = "whats_new"

    /** Newest first; the first entry is the current version's. Titles and descriptions in lowercase, like the app. */
    val releases = listOf(
        Release(
            "3.2",
            listOf(
                "neler yeni" to "güncellemeden sonra yeni gelenler burada gösterilir. ayarlar → hakkında'dan da açılır.",
                "bağlantılar" to "dış bağlantılar yüklüyse kendi uygulamasında (youtube, x...), değilse uygulama içinde açılır.",
                "google'da ara" to "başlık menüsünden başlığı google'da uygulamadan çıkmadan ara.",
                "entry yazma tam ekran" to "yazma penceresi doğrudan tam ekran açılır; gönder düğmesi üstte, bkz ve spoiler gibi araçlar yazı kutusunun üstünde.",
                "listeler yarım ya da tam ekran" to "favorileyenler ve takipçi listeleri yarım açılır, yukarı çekince tam ekran olur.",
                "görsellerde sade görünüm" to "tam ekran görsele bir kez dokununca üst çubuk gizlenir.",
                "daha akıcı geri hareketi" to "sekmelerden akışa geri dönerken sayfa parmağı takip ederek geri çekilir.",
                "düzeltmeler" to "sayfa çubuğundaki yazma düğmesinin ekranı kaplaması ve pencereler sürüklenirken görülen kararma giderildi."
            )
        ),
        Release(
            "3.1",
            listOf(
                "rastgele sekmesi" to "entry'ler sohbet gibi gelir: şükela, derinlerden ya da karışık. dokununca kendi başlığında açılır.",
                "buzlu cam ve bulanıklık" to "çubuklar arkasını bulanık gösterir, spoiler bulanık gelir. ayarlar → görünüm'den kapatılabilir.",
                "geri hareketi" to "kenardan kaydırınca sayfa parmağı takip eder, arkadaki sayfa görünür.",
                "başlığın sonunda yukarı çek" to "son sayfada yukarı çekince yeni entry'lere bakılır.",
                "başlıklar arasında kaydır" to "isteğe bağlı: bir başlıkta sağa sola kaydırarak listedeki komşu başlıklara geç.",
                "entry'lerde görseller" to "isteğe bağlı: entry'deki görseller metnin altında önizlenir. görselden entry'sine de gidilebilir.",
                "yazı ayarları" to "kalın yazı, yazı tipini her yerde kullanma ve kendi yazı tipini ekleme.",
                "güncelleme ve destek" to "yeni sürüm çıkınca haber verilir; sorun bildir ile hata kaydı açılır."
            )
        )
    )

    private val _pending = mutableStateOf<Release?>(null)
    /** The release to show now, or null */
    val pending: State<Release?> = _pending

    /** Called on start: queues this version's notes once after an update. */
    fun check(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val info = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull() ?: return
        val version = info.versionName ?: return
        val seen = prefs.getString("seen", null)
        if (seen == version) return
        prefs.edit().putString("seen", version).apply()
        // A fresh install has nothing to compare against
        if (seen == null && info.firstInstallTime == info.lastUpdateTime) return
        releases.firstOrNull { it.version == version }?.let { _pending.value = it }
    }

    /** hakkında → neler yeni */
    fun show() {
        _pending.value = releases.firstOrNull()
    }

    fun dismiss() {
        _pending.value = null
    }
}
