package dev.programmerq.bruneiazan.shared.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object PrayerJson {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class MetaWire(
        val district: String = "",
        @SerialName("offset_minutes") val offsetMinutes: Int = 0,
        @SerialName("qibla_bearing") val qiblaBearing: String = "",
    )

    @Serializable
    private data class ScheduleItemWire(
        val key: String = "",
        val name: String = "",
        @SerialName("time_24") val time24: String = "",
        @SerialName("time_12") val time12: String = "",
        val type: String = "",
    )

    @Serializable
    private data class DayWire(
        val day: String = "",
        @SerialName("date_iso") val dateIso: String = "",
        @SerialName("date_malay") val dateMalay: String = "",
        val hijri: String = "",
        val schedule: List<ScheduleItemWire> = emptyList(),
    )

    @Serializable
    private data class ErrWire(
        val status: String = "",
        val code: Int = 0,
        val message: String = "",
    )

    fun parseDay(body: String): ApiResult<Pair<Meta, PrayerDay>> {
        val root = parseRoot(body) ?: return ApiResult.Err(0, "invalid JSON")
        asError(root)?.let { return it }
        val data = root["data"] as? JsonObject
            ?: return ApiResult.Err(0, "missing data object")
        val meta = root["meta"]?.let {
            runCatching { json.decodeFromJsonElement<MetaWire>(it) }.getOrNull()
        }
        return ApiResult.Ok(meta.toMeta() to data.toDay())
    }

    fun parseMonth(body: String): ApiResult<List<PrayerDay>> {
        val root = parseRoot(body) ?: return ApiResult.Err(0, "invalid JSON")
        asError(root)?.let { return it }
        val data = root["data"] as? JsonArray
            ?: return ApiResult.Err(0, "missing data array")
        return ApiResult.Ok(data.mapNotNull { el ->
            (el as? JsonObject)?.let { runCatching { it.toDay() }.getOrNull() }
        })
    }

    private fun parseRoot(body: String): JsonObject? =
        runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()

    private fun asError(root: JsonObject): ApiResult.Err? {
        val status = root["status"]?.jsonPrimitive?.content
        if (status != "error") return null
        val err = runCatching { json.decodeFromJsonElement<ErrWire>(root) }.getOrNull()
            ?: return ApiResult.Err(0, "unknown error")
        return ApiResult.Err(err.code, err.message)
    }

    private fun MetaWire?.toMeta() = Meta(
        district = this?.district ?: "",
        offsetMinutes = this?.offsetMinutes ?: 0,
        qiblaBearing = this?.qiblaBearing ?: "",
    )

    private fun JsonObject.toDay(): PrayerDay {
        val wire = json.decodeFromJsonElement<DayWire>(this)
        return PrayerDay(
            day = wire.day,
            dateIso = wire.dateIso,
            dateMalay = wire.dateMalay,
            hijri = wire.hijri,
            schedule = wire.schedule.map {
                ScheduleItem(it.key, it.name, it.time24, it.time12, it.type)
            },
        )
    }
}
