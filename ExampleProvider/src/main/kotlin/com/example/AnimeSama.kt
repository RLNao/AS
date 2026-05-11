package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.app
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class AnimeSama : MainAPI() {
    override var name = "Anime-Sama"
    override var mainUrl = "https://anime-sama.to"
    override val supportedTypes = setOf(TvType.Anime)
    override var lang = "fr"

    // --- ÉTAPE 1 : LA RECHERCHE ---
    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val response = app.get(url).text
        val document = Jsoup.parse(response)

        return document.select("div.card-anime").mapNotNull {
            it.toSearchResult()
        }
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h5, h3")?.text() ?: return null
        val href = this.selectFirst("a")?.attr("href") ?: return null
        val posterUrl = this.selectFirst("img")?.attr("src")

        return newAnimeSearchResponse(title, href, TvType.Anime) {
            this.posterUrl = posterUrl
        }
    }

    // --- ÉTAPE 2 : CHARGEMENT DE LA FICHE ET DES ÉPISODES ---
    override suspend fun load(url: String): LoadResponse {
        val response = app.get(url).text
        val document = Jsoup.parse(response)

        // On récupère les métadonnées
        val title = document.selectFirst("h1")?.text()?.trim() ?: "Sans titre"
        val poster = document.selectFirst("img.cover")?.attr("src")
        val plot = document.selectFirst("p.synopsis")?.text()

        val episodes = mutableListOf<Episode>()

        // Scraping des épisodes (on cible les liens dans la liste des épisodes)
        document.select("div.episodes-list a").forEach {
            val name = it.text().trim()
            val link = it.attr("href")

            episodes.add(newEpisode(link) {
                this.name = name
            })
        }

        return newAnimeLoadResponse(title, url, TvType.Anime) {
            this.posterUrl = poster
            this.plot = plot
            this.addEpisodes(DubStatus.Subbed, episodes)
        }
    }

    // --- ÉTAPE 3 : EXTRACTION DES LIENS VIDÉO ---
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        // 'data' contient l'URL de l'épisode récupérée dans 'load'
        val response = app.get(data).text
        val document = Jsoup.parse(response)

        // On cherche les iframes ou les liens vers les lecteurs (Sibnet, Sendvid, MyCloud...)
        document.select("iframe").forEach { iframe ->
            val src = iframe.attr("src")
            // Cloudstream possède un système d'extracteurs automatiques
            // loadExtractor va tenter d'extraire la vidéo si le lecteur est reconnu
            loadExtractor(src, data, subtitleCallback, callback)
        }

        return true
    }
}