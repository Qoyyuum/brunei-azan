package dev.programmerq.bruneiazan.shared.api

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

internal fun monthParam(year: Int, month: Int): String = "%02d/%04d".format(month, year)

/** Injectable month fetcher so PrayerRepository stays unit-testable. */
fun interface MonthApi {
    suspend fun fetchMonth(year: Int, month: Int, district: String): ApiResult<List<PrayerDay>>
}

object WaktuApi : MonthApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetchDay(dateIso: String?, district: String): ApiResult<PrayerDay> {
        val url = "https://waktu.co.bn/api".toHttpUrl().newBuilder()
            .addQueryParameter("action", "today")
            .addQueryParameter("date", dateIso ?: "today")
            .addQueryParameter("format", "24h")
            .addQueryParameter("district", district)
            .build()
        return get(url.toString()) { body ->
            when (val r = PrayerJson.parseDay(body)) {
                is ApiResult.Ok -> ApiResult.Ok(r.value.second)
                is ApiResult.Err -> r
            }
        }
    }

    override suspend fun fetchMonth(
        year: Int,
        month: Int,
        district: String,
    ): ApiResult<List<PrayerDay>> {
        val url = "https://waktu.co.bn/api".toHttpUrl().newBuilder()
            .addQueryParameter("action", "month")
            .addQueryParameter("month", monthParam(year, month))
            .addQueryParameter("format", "24h")
            .addQueryParameter("district", district)
            .build()
        return get(url.toString()) { body -> PrayerJson.parseMonth(body) }
    }

    private suspend fun <T> get(
        url: String,
        parse: (String) -> ApiResult<T>,
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@use runCatching { PrayerJson.parseDay(body) }
                        .getOrNull()
                        ?.let { it as? ApiResult.Err }
                        ?: ApiResult.Err(resp.code, "HTTP ${resp.code}")
                }
                parse(body)
            }
        } catch (e: Exception) {
            ApiResult.Err(-1, e.message ?: "network error")
        }
    }
}
