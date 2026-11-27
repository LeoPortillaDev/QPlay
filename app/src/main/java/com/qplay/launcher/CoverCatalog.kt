package com.qplay.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer

/**
 * Catálogo público de ícones do Nintendo Switch (~20 mil jogos).
 * Formato de cada item de "games": [id, nome, ícone (URL ou hash), publisher, categorias, data].
 * Tudo aqui é bloqueante: chame sempre de Dispatchers.IO.
 */
object CoverCatalog {
    data class Hit(val name: String, val url: String)

    private class Item(val name: String, val norm: String, val words: List<String>, val url: String)

    private const val CATALOG_URL = "https://raw.githubusercontent.com/Marxally/switchicons/main/data.json"
    private const val ICON_PREFIX = "https://img-eshop.cdn.nintendo.net/i/"
    private const val ONE_DAY = 24 * 60 * 60 * 1000L

    // Catálogo já interpretado fica em memória: a busca seguinte é instantânea.
    @Volatile private var items: List<Item>? = null

    // Prévias já baixadas (até ~24 MB), para a grade não rebaixar ao rolar.
    private val bitmaps = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun clearCache(context: Context) {
        items = null
        bitmaps.evictAll()
        File(context.cacheDir, "switch-cover-catalog.json").delete()
    }

    /** Tira extensão, [colchetes], (parênteses), IDs de 16 dígitos hex e versões ("v1.0.2") do nome do arquivo. */
    fun cleanQuery(raw: String): String = raw
        .replace(Regex("\\.(nup|nsp|xci|nsz)$", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\[[^\\]]*]|\\([^)]*\\)"), " ")
        .replace(Regex("\\b[0-9a-fA-F]{16}\\b"), " ")
        .replace(Regex("\\bv\\d+(\\.\\d+)*\\b", RegexOption.IGNORE_CASE), " ")
        .replace('_', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun normalize(value: String): String =
        Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

    private fun download(target: File): String {
        val connection = URL(CATALOG_URL).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 45000
        connection.setRequestProperty("User-Agent", "QPlay-Android")
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            target.writeText(json)
            return json
        } finally {
            connection.disconnect()
        }
    }

    @Synchronized
    private fun loadItems(context: Context): List<Item> {
        items?.let { return it }
        val cache = File(context.cacheDir, "switch-cover-catalog.json")
        val hasCache = cache.exists() && cache.length() > 0L
        val json = if (hasCache && System.currentTimeMillis() - cache.lastModified() < ONE_DAY) {
            cache.readText()
        } else {
            try {
                download(cache)
            } catch (e: Exception) {
                // Sem internet: usa o catálogo antigo, se existir.
                if (hasCache) cache.readText() else throw e
            }
        }
        val games = JSONObject(json).optJSONArray("games") ?: return emptyList()
        val parsed = ArrayList<Item>(games.length())
        for (i in 0 until games.length()) {
            val entry = games.optJSONArray(i) ?: continue
            val name = entry.optString(1).replace("™", "").replace("®", "").trim()
            val ref = entry.optString(2).trim()
            if (name.isEmpty() || ref.isEmpty()) continue
            val norm = normalize(name)
            val url = if (ref.startsWith("http", ignoreCase = true)) ref else "$ICON_PREFIX$ref.jpg"
            parsed.add(Item(name, norm, norm.split(' ').filter { it.isNotEmpty() }, url))
        }
        items = parsed
        return parsed
    }

    /**
     * Busca por nome. Regras:
     * 1. todas as palavras da busca precisam aparecer (início de palavra basta: "zelda bre" acha Breath);
     * 2. nome igual > começa com a busca > contém a busca > só as palavras;
     * 3. Demo, Upgrade Pack e Kiosk vão para o fim, a menos que você os procure;
     * 4. resultados com o mesmo ícone (ex.: jogo e Demo) aparecem uma vez só;
     * 5. se nenhum jogo tem todas as palavras, mostra os que têm algumas.
     */
    fun search(context: Context, rawQuery: String, limit: Int = 60): List<Hit> {
        val cleaned = cleanQuery(rawQuery).ifBlank { rawQuery }
        val query = normalize(cleaned)
        if (query.isBlank()) return emptyList()
        val queryWords = query.split(' ').filter { it.isNotEmpty() }
        val wantsExtras = queryWords.any { it == "demo" || it == "upgrade" || it == "kiosk" || it == "pack" }

        val full = ArrayList<Pair<Int, Item>>()
        val partial = ArrayList<Pair<Int, Item>>()
        for (item in loadItems(context)) {
            val matched = queryWords.count { w -> item.words.any { it == w || it.startsWith(w) } }
            if (matched == 0) continue
            var score = when {
                item.norm == query -> 1000
                matched == queryWords.size && item.norm.startsWith(query) -> 800
                matched == queryWords.size && item.norm.contains(query) -> 700
                matched == queryWords.size -> 500
                else -> matched * 10
            }
            if (!wantsExtras && item.words.any { it == "demo" || it == "upgrade" || it == "kiosk" }) score -= 300
            score -= (item.norm.length - query.length).coerceAtLeast(0).coerceAtMost(200) // nomes mais curtos primeiro
            if (matched == queryWords.size) full.add(score to item) else partial.add(score to item)
        }
        val pool = if (full.isNotEmpty()) full else partial
        return pool
            .sortedByDescending { it.first }
            .distinctBy { it.second.url }
            .take(limit)
            .map { Hit(it.second.name, it.second.url) }
    }

    /* ---------------------------- imagens ---------------------------- */

    private fun fetchBytes(url: String, readTimeout: Int = 20000): ByteArray {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 12000
        connection.readTimeout = readTimeout
        connection.setRequestProperty("User-Agent", "QPlay-Android")
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            return connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    private fun decode(bytes: ByteArray, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample > maxSide || bounds.outHeight / sample > maxSide) sample *= 2
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    /** Prévia reduzida (com cache). Devolve null se não carregar; a tela mostra um ícone no lugar. */
    fun loadBitmap(url: String, maxSide: Int = 420): Bitmap? {
        val key = "$url@$maxSide"
        bitmaps.get(key)?.let { return it }
        val bmp: Bitmap? = try {
            decode(fetchBytes(url), maxSide)
        } catch (_: Exception) {
            null
        }
        if (bmp != null) bitmaps.put(key, bmp)
        return bmp
    }

    /** Baixa a capa em boa qualidade e salva em [target] (JPEG). Lança exceção se a imagem for inválida. */
    fun downloadTo(url: String, target: File) {
        val bmp = decode(fetchBytes(url, 30000), 1024) ?: throw IllegalStateException("Imagem inválida")
        target.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
    }
}
