package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.os.Build
import android.text.Html
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.model.AnatomyViewType
import com.example.data.model.MedicalPlateAsset
import com.example.data.model.WikimediaPhotoCategory
import com.example.data.model.WikimediaPhotoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class WikimediaPhotoPage(
    val items: List<WikimediaPhotoItem>,
    val nextContinueToken: String?
)

object WikimediaMediaRepository {

    private const val API_ENDPOINT = "https://commons.wikimedia.org/w/api.php"
    private const val USER_AGENT =
        "BloomAtlasAndroid/1.0 (https://commons.wikimedia.org; educational sexual-health anatomy app)"

    val sharedOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json, image/*")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    @Volatile
    private var cachedImageLoader: ImageLoader? = null

    fun getImageLoader(context: Context): ImageLoader {
        return cachedImageLoader ?: synchronized(this) {
            cachedImageLoader ?: ImageLoader.Builder(context.applicationContext)
                .okHttpClient(sharedOkHttpClient)
                .memoryCache {
                    MemoryCache.Builder(context.applicationContext)
                        .maxSizePercent(0.25)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.applicationContext.cacheDir.resolve("wikimedia_image_cache"))
                        .maxSizeBytes(80L * 1024L * 1024L)
                        .build()
                }
                .crossfade(true)
                .build()
                .also { cachedImageLoader = it }
        }
    }

    // Verified open-access OpenStax & Wikimedia medical illustrations for internal/clitoral/pelvic views
    private val plateCandidates: Map<AnatomyViewType, List<String>> = mapOf(
        AnatomyViewType.INTERNAL_FRONT to listOf(
            "File:Scheme female reproductive system-en.svg",
            "File:Blausen 0400 FemaleReproSystem 02.png",
            "File:Figure 28 02 01.JPG"
        ),
        AnatomyViewType.CLITORAL_NETWORK to listOf(
            "File:Clitoris Anatomy.svg",
            "File:Clitoris anatomy labeled-en.svg",
            "File:Anatomy of clitoris and associated structures.svg"
        ),
        AnatomyViewType.PELVIC_SIDE to listOf(
            "File:Figure 28 02 01.JPG",
            "File:Female anatomy with g-spot-en.svg",
            "File:Female reproductive system lateral.png"
        )
    )

    suspend fun fetchMedicalPlates(): Map<AnatomyViewType, MedicalPlateAsset> = withContext(Dispatchers.IO) {
        val allTitles = plateCandidates.values.flatten().distinct()
        val url = Uri.parse(API_ENDPOINT).buildUpon()
            .appendQueryParameter("action", "query")
            .appendQueryParameter("format", "json")
            .appendQueryParameter("titles", allTitles.joinToString("|"))
            .appendQueryParameter("prop", "imageinfo")
            .appendQueryParameter("iiprop", "url|size|mime|extmetadata")
            .appendQueryParameter("iiurlwidth", "1100")
            .build()
            .toString()

        val body = executeGet(url) ?: return@withContext emptyMap()
        val root = JSONObject(body)
        val query = root.optJSONObject("query") ?: return@withContext emptyMap()
        val pages = query.optJSONObject("pages") ?: return@withContext emptyMap()

        // Index resolved files by normalized lowercase title
        val resolvedByTitle = mutableMapOf<String, MedicalPlateAsset>()
        val keys = pages.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val page = pages.optJSONObject(key) ?: continue
            if (page.has("missing")) continue
            val title = page.optString("title", "")
            val imageInfoArray = page.optJSONArray("imageinfo") ?: continue
            if (imageInfoArray.length() == 0) continue
            val info = imageInfoArray.getJSONObject(0)
            val thumbUrl = info.optString("thumburl").ifBlank { info.optString("url") }
            if (thumbUrl.isBlank() || !thumbUrl.startsWith("https://")) continue

            val ext = info.optJSONObject("extmetadata")
            val artist = extractMetaText(ext, "Artist").ifBlank { "OpenStax / Wikimedia Commons" }
            val license = extractMetaText(ext, "LicenseShortName").ifBlank { "CC BY / Open Access" }
            val descUrl = info.optString("descriptionurl", "https://commons.wikimedia.org/")

            resolvedByTitle[normalizeTitle(title)] = MedicalPlateAsset(
                viewType = AnatomyViewType.EXTERNAL_VULVA,
                imageUrl = thumbUrl,
                attribution = artist,
                license = license,
                sourcePageUrl = descUrl
            )
        }

        val result = mutableMapOf<AnatomyViewType, MedicalPlateAsset>()
        plateCandidates.forEach { (viewType, candidates) ->
            for (candidate in candidates) {
                val match = resolvedByTitle[normalizeTitle(candidate)]
                if (match != null) {
                    result[viewType] = match.copy(viewType = viewType)
                    break
                }
            }
        }

        // Fetch a clean, single undisturbed real vulva photograph for the optional Photo Reference toggle
        fetchSingleUndisturbedVulvaPhoto()?.let { singlePhoto ->
            result[AnatomyViewType.EXTERNAL_VULVA] = singlePhoto
        }

        result
    }

    /**
     * Fetches a single, undisturbed real photograph from Wikimedia Commons for the optional
     * External Vulva Photo Reference toggle (avoiding two-panel diagrams or hand-spread views).
     */
    private suspend fun fetchSingleUndisturbedVulvaPhoto(): MedicalPlateAsset? {
        val page = fetchDiversityPhotos(
            category = WikimediaPhotoCategory.ALL_VULVAS,
            continueToken = null,
            limit = 24
        ).getOrNull() ?: return null

        val excludedKeywords = listOf(
            "hand", "finger", "spread", "open", "pull", "diagram", "scheme",
            "clitoris", "pierc", "tattoo", "shav", "surgery", "medical", "collage",
            "comparison", "mosaic", "wall", "figure", "labeled", "notext"
        )

        val candidate = page.items.firstOrNull { item ->
            val hay = "${item.title} ${item.caption}".lowercase()
            val cleanSubject = excludedKeywords.none { kw -> hay.contains(kw) }
            val balancedPortrait = item.aspectRatio in 0.68f..1.18f
            cleanSubject && balancedPortrait
        } ?: page.items.firstOrNull { item ->
            val hay = item.title.lowercase()
            excludedKeywords.none { kw -> hay.contains(kw) }
        } ?: page.items.firstOrNull()

        return candidate?.let { photo ->
            MedicalPlateAsset(
                viewType = AnatomyViewType.EXTERNAL_VULVA,
                imageUrl = photo.thumbUrl,
                attribution = photo.author,
                license = photo.license,
                sourcePageUrl = photo.pageUrl
            )
        }
    }

    suspend fun fetchDiversityPhotos(
        category: WikimediaPhotoCategory,
        continueToken: String? = null,
        limit: Int = 18
    ): Result<WikimediaPhotoPage> = withContext(Dispatchers.IO) {
        try {
            val builder = Uri.parse(API_ENDPOINT).buildUpon()
                .appendQueryParameter("action", "query")
                .appendQueryParameter("format", "json")
                .appendQueryParameter("generator", "categorymembers")
                .appendQueryParameter("gcmtitle", category.commonsCategory)
                .appendQueryParameter("gcmtype", "file")
                .appendQueryParameter("gcmlimit", limit.toString())
                .appendQueryParameter("prop", "imageinfo")
                .appendQueryParameter("iiprop", "url|size|mime|extmetadata")
                .appendQueryParameter("iiurlwidth", "880")
            if (!continueToken.isNullOrBlank()) {
                builder.appendQueryParameter("gcmcontinue", continueToken)
            }

            val body = executeGet(builder.build().toString())
                ?: return@withContext Result.failure(IllegalStateException("Network request failed"))

            val root = JSONObject(body)
            val nextToken = root.optJSONObject("continue")?.optString("gcmcontinue")
                ?.takeIf { it.isNotBlank() }

            val pages = root.optJSONObject("query")?.optJSONObject("pages")
                ?: return@withContext Result.success(WikimediaPhotoPage(emptyList(), null))

            val parsed = mutableListOf<WikimediaPhotoItem>()
            val keys = pages.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val page = pages.optJSONObject(key) ?: continue
                val pageId = page.optLong("pageid", -1L)
                if (pageId <= 0L) continue
                val rawTitle = page.optString("title", "")
                val imageInfoArr = page.optJSONArray("imageinfo") ?: continue
                if (imageInfoArr.length() == 0) continue
                val info = imageInfoArr.getJSONObject(0)

                val mime = info.optString("mime", "").lowercase()
                val isValidMime = if (category.photosOnly) {
                    mime == "image/jpeg" || mime == "image/png" || mime == "image/webp"
                } else {
                    mime.startsWith("image/")
                }
                if (!isValidMime) continue

                val width = info.optInt("thumbwidth", info.optInt("width", 0))
                val height = info.optInt("thumbheight", info.optInt("height", 0))
                val origWidth = info.optInt("width", width)
                if (origWidth in 1..220) continue

                val thumbUrl = info.optString("thumburl").ifBlank { info.optString("url") }
                val fullUrl = info.optString("url").ifBlank { thumbUrl }
                if (!thumbUrl.startsWith("https://")) continue

                val ext = info.optJSONObject("extmetadata")
                val rawDesc = extractMetaText(ext, "ImageDescription")
                val author = extractMetaText(ext, "Artist").ifBlank { "Wikimedia Commons Contributor" }
                val license = extractMetaText(ext, "LicenseShortName").ifBlank { "Creative Commons" }
                val pageUrl = info.optString("descriptionurl", "https://commons.wikimedia.org/")

                val cleanDisplayTitle = cleanFilename(rawTitle)
                val cleanCaption = rawDesc.takeIf { it.length >= 12 }
                    ?: "Educational anatomical reference photograph from Wikimedia Commons (${category.label})."

                parsed.add(
                    WikimediaPhotoItem(
                        pageId = pageId,
                        title = rawTitle,
                        displayTitle = cleanDisplayTitle,
                        caption = cleanCaption.take(260),
                        thumbUrl = thumbUrl,
                        fullUrl = if (mime == "image/svg+xml") thumbUrl else fullUrl,
                        pageUrl = pageUrl,
                        width = width,
                        height = height,
                        author = author.take(80),
                        license = license.take(40)
                    )
                )
            }

            // Sort deterministically by pageId so pagination order stays stable
            parsed.sortBy { it.pageId }
            Result.success(WikimediaPhotoPage(items = parsed, nextContinueToken = nextToken))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun executeGet(url: String): String? {
        val request = Request.Builder().url(url).get().build()
        sharedOkHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()
        }
    }

    private fun normalizeTitle(title: String): String =
        title.trim().replace('_', ' ').lowercase()

    private fun cleanFilename(raw: String): String {
        return raw.removePrefix("File:")
            .substringBeforeLast('.')
            .replace('_', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { "Anatomical Reference" }
    }

    private fun extractMetaText(ext: JSONObject?, field: String): String {
        val rawHtml = ext?.optJSONObject(field)?.optString("value").orEmpty()
        if (rawHtml.isBlank()) return ""
        val plain = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(rawHtml, Html.FROM_HTML_MODE_COMPACT).toString()
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(rawHtml).toString()
        }
        return plain.replace(Regex("\\s+"), " ").trim()
    }
}
