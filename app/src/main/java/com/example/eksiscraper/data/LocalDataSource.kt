package com.example.eksiscraper.data

import com.example.eksiscraper.model.Entry
import com.example.eksiscraper.model.HomeResponse
import com.example.eksiscraper.model.Topic

object LocalDataSource {
    fun getLocalData() = HomeResponse(
        home = listOf(
            Topic(
                title = "kahve içmeden güne başlayamayanlar",
                url = "/kahve-icmeden-gune-baslayamayanlar",
                commentCount = 120,
                entries = listOf(
                    Entry("kahve olmadan sabahları beynim çalışmıyor. resmen yaşam destek ünitem."),
                    Entry("kahve bağımlılığı diye bir şey varsa kesinlikle bende var. gün içinde 4-5 bardak içmeden olmuyor.")
                ),
                entriesLoaded = true
            ),
            Topic(
                title = "ekşi sözlük",
                url = "/eksi-sozluk",
                commentCount = 250,
                entries = listOf(
                    Entry("1999 yılından beri var olan interaktif sözlük."),
                    Entry("türkiye'nin en büyük sosyal medya platformlarından biri.")
                ),
                entriesLoaded = true
            ),
            Topic(
                title = "android uygulamaları",
                url = "/android-uygulamalari",
                commentCount = 85,
                entries = listOf(
                    Entry("play store'da milyonlarca uygulama var ama çoğu gereksiz."),
                    Entry("en çok kullandığım uygulamalar: spotify, whatsapp, instagram.")
                ),
                entriesLoaded = true
            )
        )
    )
} 