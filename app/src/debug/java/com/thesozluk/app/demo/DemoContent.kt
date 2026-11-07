package com.thesozluk.app.demo

import com.thesozluk.app.model.AuthorImage
import com.thesozluk.app.model.AuthorProfile
import com.thesozluk.app.model.Badge
import com.thesozluk.app.model.Channel
import com.thesozluk.app.model.Comment
import com.thesozluk.app.model.Entry
import com.thesozluk.app.model.FollowUser
import com.thesozluk.app.model.FormSpec
import com.thesozluk.app.model.Message
import com.thesozluk.app.model.MessageBox
import com.thesozluk.app.model.MessageThread
import com.thesozluk.app.model.ThreadDetail
import com.thesozluk.app.model.Topic
import com.thesozluk.app.network.DemoSource
import com.thesozluk.app.network.RelationKind
import com.thesozluk.app.network.RelationPage
import com.thesozluk.app.network.RelationUser
import com.thesozluk.app.network.TrashEntry
import com.thesozluk.app.network.TrashPage

/**
 * The demo's content, in the debug build only (src/debug): everything here is made up for
 * screenshots. Nicks are invented, not taken from the site, and the writing is our own.
 */
object DemoContent : DemoSource {

    override val ownNick = "demo hesap"

    private val authors = listOf(
        "pencere kenarı kaktüsü", "sabah treninin yolcusu", "yarım kalmış bulmaca", "uykusuz baykuş",
        "kayıp çorap dedektifi", "kağıttan uçak pilotu", "çay demleyen robot", "son durak çaycısı",
        "pazartesi direnişçisi", "kırmızı şemsiyeli yolcu"
    )

    private class DemoTopic(val title: String, val today: Int, val entries: List<Pair<String, String>>)

    private fun bkz(word: String) = "(bkz: <a class=\"b\" href=\"/?q=${word.replace(' ', '+')}\">$word</a>)"

    private val pool = listOf(
        DemoTopic("the sözlük", 42, listOf(
            "" to "ekşi sözlük'ü telefonda okumak için yazılmış, resmi olmayan, açık kaynaklı bir android uygulaması. başlıkları, entry'leri ve profilleri sitenin kendisinden okur.",
            "" to "karanlık modda gece okumak için birebir. yazı tipi, satır aralığı ve kenar boşluğu ayarlanabiliyor, göz yormuyor.",
            "" to "${bkz("material 3")}<br><br>renk paletini değiştirince bütün uygulama o renge bürünüyor. duvar kâğıdından renk alma seçeneği de var.",
            "" to "sıvı cam açınca çubuklar, pencereler ve alt sayfalar cam gibi oluyor, kenarları arkadan geçen renkleri yansıtıyor. ayarlarda canlı önizlemesi bile var.",
            "" to "alttaki çubukta hap parmağı takip ediyor. çubuğun üstünde sürükleyerek sekme değiştirmek ayrı bir keyif.",
            "" to "bir entry'ye basılı tutunca öne çıkıyor; favorile, paylaş, kaydet, hepsi hemen altında.",
            "" to "birden fazla başlığı tarayıcıdaki gibi sekmelerde açık tutabiliyorsun. uygulama kapansa da açık sekmeleri hatırlıyor.",
            "" to "kendi profilinde biyografini yazıp fotoğrafını kırparak değiştirebiliyorsun. öne çıkan rozetler de sürükleyerek seçiliyor.",
            "" to "sildiğin entry'ler çöp kutusunda duruyor, tek dokunuşla geri geliyor. ${bkz("çöp kutusu")}",
            "" to "başlıkları çevrimdışı kaydedip internet yokken okumak mümkün. metroda çok işe yarıyor.",
            "" to "ana sayfadaki listeleri istediğin sıraya dizebiliyorsun, gündem en üstte olmak zorunda değil.",
            "" to "spoiler'lar kapalı geliyor, dokununca açılıyor. kimse dizinin sonunu istemeden öğrenmiyor."
        )),
        DemoTopic("sabah kahvesi mi çay mı", 128, listOf(
            "kahve" to "güne kahveyle başlamayınca gözler açılmıyor. ama ikinci fincandan sonra iş bambaşka bir yere gidiyor, ölçüyü kaçırmamak lazım.",
            "çay" to "demlik ocakta fokurdarken mutfağa yayılan koku başlı başına bir uyandırma servisi. ince belli bardakta, tavşan kanı.",
            "ikisi" to "sabah çay, öğleden sonra kahve. bu düzen bozulduğu gün her şey ters gidiyor, deneyen bilir. ${bkz("rutin")}"
        )),
        DemoTopic("kediler neden kutuları sever", 96, listOf(
            "" to "kutu ne kadar küçükse o kadar iyi. sığmıyor olması da kediyi hiç ilgilendirmiyor, yarısı dışarıda uyuyor.",
            "" to "yeni aldığınız pahalı yatağı değil, onun geldiği kutuyu seçecektir. bu bir kural. ${bkz("kedi mantığı")}",
            "" to "güvende hissetmek için diyorlar. bizimki kutuya girip bize tepeden bakıyor, güvenden çok hükümranlık var gibi."
        )),
        DemoTopic("yağmurlu havada okunacak kitaplar", 74, listOf(
            "" to "cam kenarı, battaniye, yanında çay. kitap ne olursa olsun yağmur sesiyle birlikte iki kat güzel okunuyor.",
            "" to "kalın bir roman tam zamanı. kısa öyküler yağmur dinene kadar bitiyor, sonra ne yapacağınızı bilemiyorsunuz.",
            "" to "eski bir polisiye. sayfalar sarardıkça, dışarısı karardıkça gerilim artıyor."
        )),
        DemoTopic("bisikletle işe gitmek", 61, listOf(
            "" to "ilk hafta bacaklar isyan ediyor, ikinci hafta trafikte bekleyenlere acımaya başlıyorsunuz.",
            "" to "yağmurda yedek kıyafet şart. bunu bir kere öğrenen bir daha unutmuyor. ${bkz("ıslak çorap")}",
            "" to "sabah serin havada pedal çevirmek kafayı öyle bir boşaltıyor ki işe vardığınızda güne hazırsınız."
        )),
        DemoTopic("pazar kahvaltısı", 55, listOf(
            "" to "saat on bir gibi başlayıp öğlen ikiye kadar süren, bitmeyen bir sofra. çay demliği üç kere tazelenir.",
            "" to "menemen soğanlı mı soğansız mı tartışması da bu kahvaltının ayrılmaz parçası.",
            "" to "pazar kahvaltısının en güzel yanı acele etmemek. saat kimsenin umurunda değil."
        )),
        DemoTopic("eski telefonların nostaljisi", 47, listOf(
            "" to "şarjı bir hafta giden, yere düşünce yeri çatlatan telefonlar. yılan oyununda rekor kırmak bir statüydü.",
            "" to "tuşlu telefonda mesaj yazma hızı ayrı bir yetenekti. şimdi o hız kimseye bir şey ifade etmiyor.",
            "" to "çekmecede duran eski telefonu bulup açınca gelen o tanıdık açılış sesi bir anda yılları geri getiriyor."
        )),
        DemoTopic("gece yarısı atıştırmalıkları", 39, listOf(
            "" to "buzdolabının ışığı yandığı an verilen kararlar sabah pişmanlık olarak geri döner.",
            "" to "en tehlikelisi 'bir tane alıp yatacağım' diye açılan paket. bir tane hiçbir zaman bir tane olmuyor.",
            "" to "gece yarısı yenen ekmek arası peynir, gündüz yenenden on kat lezzetli. bilimsel açıklaması yok."
        )),
        DemoTopic("otobüste cam kenarı", 33, listOf(
            "" to "boş otobüste bile herkesin önce cam kenarına yürüdüğü bir yarış.",
            "" to "kulaklık takıp camdan dışarı bakınca kendini bir filmin içinde sanmak serbest.",
            "" to "kış sabahı buğulu cama parmakla bir şeyler çizmek de bu koltuğun ayrıcalıkları arasında."
        )),
        DemoTopic("ev bitkisi bakımı", 28, listOf(
            "" to "en çok sevdiğin bitki en çabuk kuruyandır. az sulanması gereken her şeyi fazla suluyoruz.",
            "" to "yeni yaprak çıkardığında duyulan sevinç anlatılmaz. sanki çocuğun ilk adımı. ${bkz("sukulent")}",
            "" to "pencere önünü güneşe göre üç kere yer değiştirdikten sonra mutlu olduğu köşeyi buluyorsunuz."
        )),
        DemoTopic("yeni yıl kararları", 24, listOf(
            "" to "spor salonu üyeliği ocak ayında doluyor, şubatta salon yine bomboş.",
            "" to "bu yıl tek bir karar aldım: karar almamak. şimdiye kadar başarıyla sürdürüyorum.",
            "" to "küçük kararlar daha kalıcı oluyor. 'her gün on sayfa okumak' 'yılda elli kitap'tan çok daha iyi işliyor."
        )),
        DemoTopic("uzun yol şarkıları", 19, listOf(
            "" to "yolculuğun ilk saatinde herkes listesini çalmak ister, üçüncü saatte radyoya razı olunur.",
            "" to "gece yolunda çalan sakin bir şarkı, yol çizgileri ve uzaklardaki ışıklar. tarif edilemez bir huzur.",
            "" to "hep birlikte bağıra çağıra söylenen o tek şarkı her yolculukta mutlaka çalar."
        )),
        DemoTopic("ilk maaşla alınan şey", 15, listOf(
            "" to "anneye bir hediye. kimse bunu planlamıyor ama herkes yapıyor.",
            "" to "kendime çok istediğim bir kulaklık aldım, yıllar sonra hâlâ kullanıyorum ve her seferinde o günü hatırlıyorum.",
            "" to "faturalar. ilk maaşın gerçeklerle tanıştırdığı an. ${bkz("yetişkinlik")}"
        ))
    )

    private fun slug(title: String) = title
        .replace('ç', 'c').replace('ğ', 'g').replace('ı', 'i').replace('ö', 'o').replace('ş', 's').replace('ü', 'u')
        .replace(' ', '-')

    private fun topicUrl(i: Int) = "/${slug(pool[i].title)}--${7000 + i}"

    private fun entryId(topic: Int, n: Int) = (900000 + topic * 100 + n).toString()

    private fun entriesOf(i: Int): List<Entry> = pool[i].entries.mapIndexed { n, (_, text) ->
        val author = authors[(i * 3 + n) % authors.size].let { if (i == 1 && n == 2) ownNick else it }
        val plain = text.replace(Regex("<[^>]+>"), "")
        Entry(
            content = plain,
            contentHtml = text,
            author = author,
            authorId = (5000 + authors.indexOf(author).coerceAtLeast(0)).toString(),
            date = "${((n * 3 + i) % 27 + 1).toString().padStart(2, '0')}.${if (n < 6) "09" else "10"}.2026 ${(8 + n * 2) % 24}:${(n * 17 % 60).toString().padStart(2, '0')}",
            favoriteCount = listOf(42, 17, 8, 3, 25)[(i + n) % 5],
            entryId = entryId(i, n),
            isFavorited = n == 1 && i % 2 == 0,
            flags = if (author == ownNick) setOf("vote", "share", "deleteself", "edit") else setOf("vote", "share"),
            topicTitle = pool[i].title,
            topicUrl = topicUrl(i)
        )
    }

    private const val PAGE_SIZE = 6

    private fun pagesOf(i: Int) = (pool[i].entries.size + PAGE_SIZE - 1) / PAGE_SIZE

    private fun topicAt(i: Int, page: Int = 1) = Topic(
        title = pool[i].title,
        url = topicUrl(i),
        commentCount = pool[i].today,
        entries = entriesOf(i).chunked(PAGE_SIZE).getOrNull(page - 1).orEmpty(),
        entriesLoaded = true,
        totalPages = pagesOf(i),
        currentPage = page.coerceIn(1, pagesOf(i)),
        topicPath = topicUrl(i),
        topicId = (7000 + i).toString(),
        entryForm = FormSpec(action = "/entry/ekle", fields = emptyMap(), textFieldName = "Content")
    )

    override fun topics(category: String, page: Int): List<Topic> {
        if (page > 1) return emptyList()
        // Each list in its own order, so the tabs don't all look the same
        val order = when {
            category.contains("debe") -> pool.indices.reversed().toList()
            category.contains("today") || category.contains("bugun") -> pool.indices.sortedBy { (it * 7) % pool.size }
            category == "popular" -> pool.indices.toList()
            else -> pool.indices.sortedBy { (it * 5 + category.length) % pool.size }
        }
        return order.map { i -> topicAt(i).copy(entries = emptyList(), entriesLoaded = false) }
    }

    override fun topic(query: String, page: Int, path: String): Topic {
        val q = query.trim().lowercase()
        val byEntry = Regex("/entry/(\\d+)").find(path)?.groupValues?.get(1) ?: q.removePrefix("#").takeIf { q.startsWith("#") }
        val index = when {
            byEntry != null -> pool.indices.firstOrNull { i -> entriesOf(i).any { it.entryId == byEntry } }
            path.isNotBlank() -> pool.indices.firstOrNull { topicUrl(it) == path.substringBefore('?') }
            else -> null
        } ?: pool.indices.firstOrNull { pool[it].title == q }
            ?: pool.indices.firstOrNull { q.isNotEmpty() && pool[it].title.contains(q) }
        if (index == null) {
            // A search with no match: an empty new topic, ready to be written
            return Topic(title = query, entriesLoaded = true, entryForm = FormSpec("/entry/ekle", emptyMap(), "Content"))
        }
        val topic = topicAt(index, page)
        return if (byEntry != null && path.startsWith("/entry/")) topic.copy(entries = topic.entries.filter { it.entryId == byEntry })
        else topic
    }

    override fun suggestions(query: String): Pair<List<String>, List<String>> {
        val q = query.trim().lowercase()
        return pool.map { it.title }.filter { it.contains(q) }.take(6) to authors.filter { it.contains(q) }.take(3).map { "@$it" }
    }

    private val badgeImages = listOf("kanuni", "bkz-fevkalbeser", "vecihi")
    private val badgeNames = listOf("kanuni", "bkz. fevkalbeşer", "vecihi")

    override fun profile(nick: String): AuthorProfile {
        val own = nick.equals(ownNick, ignoreCase = true)
        val seed = nick.length
        val badges = badgeImages.indices.map { i ->
            Badge(
                name = badgeNames[i],
                description = "örnek rozet açıklaması",
                imageUrl = "https://cdn.eksisozluk.com/badges/${badgeImages[i]}.png",
                id = (10 + i).toString()
            )
        }
        return AuthorProfile(
            nick = nick,
            avatarUrl = null,
            isVerified = seed % 4 == 0,
            karma = if (own) "çaylak dostu (312)" else "sözlük emektarı (${400 + seed * 7})",
            biography = if (own) "uygulamanın ekran görüntüleri için uydurulmuş bir hesap. buradaki her şey örnek."
            else "sabahları çay, akşamları kitap. arada bir de yazıyorum.",
            entryCount = 120 + seed * 13,
            followerCount = 30 + seed * 3,
            followingCount = 12 + seed,
            joinedDate = "mart 2019",
            badges = badges,
            allBadges = badges + Badge("gece kuşu", "gece yazanlar", "https://cdn.eksisozluk.com/badges/gece-kusu.png", owned = false, id = "20"),
            isFollowing = !own && seed % 2 == 0
        )
    }

    override fun userEntries(nick: String, tab: String, page: Int): List<Entry> {
        if (page > 1) return emptyList()
        return pool.indices.flatMap { entriesOf(it) }.filter { it.author == nick }.ifEmpty {
            pool.indices.take(4).map { i -> entriesOf(i).first().copy(author = nick) }
        }
    }

    override fun userImages(nick: String): List<AuthorImage> = emptyList()

    override fun comments(entryId: String): List<Comment> = emptyList()

    override fun favoriters(entryId: String): List<String> = authors.shuffled(kotlin.random.Random(entryId.hashCode())).take(6)

    override fun followList(nick: String, following: Boolean): List<FollowUser> =
        authors.drop(if (following) 0 else 4).take(6).mapIndexed { i, n -> FollowUser(n, null, i == 1, id = (5100 + i).toString()) }

    private val threads = listOf(
        Triple(authors[1], "yarınki buluşma hâlâ geçerli mi?", "14:22"),
        Triple(authors[4], "önerdiğin kitabı bitirdim, sonu harikaydı", "dün"),
        Triple(authors[7], "çay bahçesinin adını hatırlıyor musun?", "salı"),
        Triple(authors[2], "bulmacanın son kelimesi neydi", "3 gün önce")
    )

    override fun messageBox() = MessageBox(
        threads = threads.mapIndexed { i, (nick, preview, time) -> MessageThread("t$i", nick, 3 + i, preview, time) },
        sendForm = FormSpec("/mesaj/sendajax", emptyMap(), "Message"),
        threadForm = null
    )

    override fun thread(id: String): ThreadDetail {
        val i = id.removePrefix("t").toIntOrNull()?.coerceIn(0, threads.lastIndex) ?: 0
        val (nick, preview, _) = threads[i]
        val lines = listOf(
            Triple("selam, nasılsın?", "10:02", false),
            Triple("iyiyim, sen nasılsın? uzun zaman oldu", "10:05", true),
            Triple(preview, "10:07", false),
            Triple("evet, bence harika olur. akşam yazarım", "10:12", true)
        )
        return ThreadDetail(
            nick = nick,
            messages = lines.map { (text, time, out) -> Message(text, text, time, out) },
            sendForm = FormSpec("/mesaj/sendajax", emptyMap(), "Message"),
            threadForm = null
        )
    }

    override fun channels(): List<Channel> = listOf(
        Channel("#gündelik", "günlük hayat, ufak tefek", "basliklar/kanal/gundelik"),
        Channel("#kitap", "okuduklarımız, okuyacaklarımız", "basliklar/kanal/kitap"),
        Channel("#yemek", "mutfaktan haberler", "basliklar/kanal/yemek"),
        Channel("#teknoloji", "eski yeni her alet", "basliklar/kanal/teknoloji"),
        Channel("#seyahat", "yollar ve duraklar", "basliklar/kanal/seyahat")
    )

    override fun trash(page: Int): TrashPage {
        if (page > 1) return TrashPage(emptyList(), page, 1, "")
        val items = listOf(
            Triple("pazar kahvaltısı", "simit taze olmadan kahvaltı başlamaz. bu konuda tartışmaya kapalıyım.", "02.10.2026 21:26"),
            Triple("otobüste cam kenarı", "bir gün koridor tarafını seçeceğim ve dünya bunu konuşacak.", "28.09.2026 08:14"),
            Triple("yeni yıl kararları", "erken kalkacağım. bu entry'yi de bu yüzden sildim, gece yazmıştım.", "03.01.2026 02:40")
        )
        return TrashPage(
            entries = items.mapIndexed { i, (title, text, date) -> TrashEntry("98000$i", title, "geçen ay", text, text, date) },
            page = 1,
            pageCount = 1,
            total = "${items.size} entry çöpte."
        )
    }

    override fun relations(kind: RelationKind, page: Int): RelationPage {
        if (page > 1) return RelationPage(emptyList(), isLast = true)
        val names = when (kind) {
            RelationKind.Followed -> authors.take(5)
            RelationKind.Blocked -> listOf("gürültücü komşu", "spoiler verici")
            RelationKind.TopicsBlocked -> listOf("her başlığa anket açan")
            RelationKind.Muted -> emptyList()
        }
        return RelationPage(names.mapIndexed { i, n -> RelationUser((5200 + i).toString(), n, null, i == 0) }, isLast = true)
    }
}
