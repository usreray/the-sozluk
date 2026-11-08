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
            "3.4",
            listOf(
                "birden fazla hesap" to "profil → hesap değiştir: bu cihazda giriş yaptığın hesaplar arasında tek dokunuşla geç, yeni hesap ekle ya da kaldır.",
                "yorumlar" to "entry altındaki yorumlar artık entry'ler gibi: yazarın adı, tarihi ve profil resmi sağ altta.",
                "kart stili" to "ayarlar → okuma → entry'ler: entry kartları sitedeki gibi klasik ya da sosyal medya akışı gibi görünebilir; profil resmi ve yazar en üstte.",
                "ilk ve son sayfa" to "sayfa çubuğunda tek tek ilerlemenin yanında başlığın ilk ve son sayfasına atlayan oklar var.",
                "akıcı listeler" to "listeler ilk açılışta satır satır belirir.",
                "sıvı cam" to "kapatılınca alt çubuk da diğer çubuklar gibi sade buzlu cama döner.",
                "küçük düzeltmeler" to "basılı tutunca açılan menü sadeleşti; uzun başlıklar üst çubuktaki düğmelerin üstüne taşmıyor; yükleniyor kartları da cam kenarlı; öne çıkan rozetlerin yanında gereksiz \"+0\" yazmıyor."
            )
        ),
        Release(
            "3.3",
            listOf(
                "profilini düzenle" to "kendi profilinde biyografini yaz, fotoğrafını kırparak değiştir, öne çıkan rozetlerini sürükleyerek seç. bütün rozetlerin de görünür.",
                "çöp kutusu" to "profilindeki ⋮ menüsünden: sildiğin entry'leri canlandır ya da tamamen sil.",
                "takip / engellenmiş" to "takip ettiklerin, engellediklerin, başlıklarını engellediklerin ve sessize aldıkların tek yerde; buradan kaldırabilirsin. takip listende takipten de çıkabilirsin.",
                "şikayet et" to "başkasının entry menüsünde: ekşi'nin şikayet formu açılır.",
                "yeni alt çubuk" to "hap parmağını izler: çubukta sürükleyerek sekme değiştir. aşağı kaydırınca yazılar sadeleşir; ayarlarda çubuğun önizlemesi var.",
                "sıvı cam" to "çubuklar, düğmeler, menüler, pencereler, alt sayfalar ve kartlar cam olur, kenarları arkadan geçen renkleri yansıtır. varsayılan olarak açık; ayarlar → görünüm'de canlı önizlemesi var, oradan kapatılabilir.",
                "yeni alt sayfalar" to "alt sayfalar ekranın kenarından ayrık, havada duran kartlar oldu; filtrele seçenekleri ikonlu satırlar halinde.",
                "entry önizleme" to "bir entry'ye basılı tut: öne çıkar, favorile, paylaş, kaydet gibi eylemler altında açılır.",
                "listeleri sırala" to "ana sayfadaki gündem, bugün, debe gibi listeleri ayarlar → ana sayfa'da sürükleyerek sırala.",
                "yüzen bildirimler" to "kaydedildi, kopyalandı gibi bildirimler çubukların üstünde kart olarak gelir; sağa ya da sola kaydırıp kapatabilirsin.",
                "başlığın sonu" to "son sayfada yukarı çekince yükleme şekilleri belirir ve yeni entry'lere bakılır.",
                "küçük güzellikler" to "sekmelerden geri dönerken sayfanın köşeleri yuvarlanır, boş ekranlarda çizimler var, tek entry ekranında başlığa dokununca başlığın ilk sayfası açılır, ayarlarda renkler en üstte."
            )
        ),
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
        // Nor one that gets the first-run guide: a reinstall can bring back an older "seen"
        // from Android's backup, which made the notes show over the guide
        if (com.thesozluk.app.ui.components.Onboarding.needed.value) return
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
