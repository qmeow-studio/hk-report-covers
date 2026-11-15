package com.tx.reportcovers

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

data class Report(
    val id: String,
    val code: String,
    val name: String,
    val title: String,
    val type: String,   // AR = 年報, IR = 中期報告
    val date: String,
    val pdf: String,
    val cover: String,
)

object DataSource {
    private lateinit var prefs: SharedPreferences
    private lateinit var cacheDir: File

    fun init(ctx: Context) {
        prefs = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
        cacheDir = File(ctx.cacheDir, "index").apply { mkdirs() }
    }

    var repo: String
        get() = (prefs.getString("repo", "") ?: "").ifBlank { BuildConfig.DATA_REPO }
        set(v) { prefs.edit().putString("repo", v.trim()).apply() }

    var token: String
        get() = (prefs.getString("token", "") ?: "").ifBlank { BuildConfig.DATA_TOKEN }
        set(v) { prefs.edit().putString("token", v.trim()).apply() }

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain ->
                val req = chain.request()
                val t = token
                if (t.isNotBlank() && req.url.host == "api.github.com") {
                    chain.proceed(
                        req.newBuilder()
                            .header("Authorization", "Bearer $t")
                            .header("Accept", "application/vnd.github.raw+json")
                            .header("X-GitHub-Api-Version", "2022-11-28")
                            .build()
                    )
                } else {
                    chain.proceed(req)
                }
            })
            .build()
    }

    /** 私人 repo（有 token）行 GitHub API；公開 repo 直接行 raw。 */
    fun url(path: String): String =
        if (token.isBlank()) "https://raw.githubusercontent.com/$repo/data/$path"
        else "https://api.github.com/repos/$repo/contents/$path?ref=data"

    private fun fetchText(path: String): String {
        val cache = File(cacheDir, path.replace('/', '_'))
        try {
            val text = client.newCall(Request.Builder().url(url(path)).build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IOException(
                        when (resp.code) {
                            401, 403 -> "無權限讀取（HTTP ${resp.code}），請檢查 token"
                            404 -> "搵唔到資料（HTTP 404）：repo 未有封面資料，或者私人 repo 未填 token"
                            else -> "HTTP ${resp.code}"
                        }
                    )
                }
                resp.body?.string() ?: throw IOException("空白回應")
            }
            cache.writeText(text)
            return text
        } catch (e: IOException) {
            if (cache.exists()) return cache.readText()
            throw e
        }
    }

    fun years(): List<Pair<Int, Int>> {
        val arr = JSONArray(fetchText("years.json"))
        val out = ArrayList<Pair<Int, Int>>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(o.getInt("year") to o.optInt("count"))
        }
        return out.sortedByDescending { it.first }
    }

    fun reports(year: Int): List<Report> {
        val arr = JSONArray(fetchText("index/$year.json"))
        val out = ArrayList<Report>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                Report(
                    id = o.optString("id"),
                    code = o.optString("code"),
                    name = o.optString("name"),
                    title = o.optString("title"),
                    type = o.optString("type"),
                    date = o.optString("date"),
                    pdf = o.optString("pdf"),
                    cover = o.optString("cover"),
                )
            )
        }
        return out
    }
}
