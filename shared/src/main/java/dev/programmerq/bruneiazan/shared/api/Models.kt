package dev.programmerq.bruneiazan.shared.api

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleItem(
    val key: String,
    val name: String,
    val time24: String,
    val time12: String,
    val type: String,
)

@Serializable
data class PrayerDay(
    val day: String,
    val dateIso: String,
    val dateMalay: String,
    val hijri: String,
    val schedule: List<ScheduleItem>,
)

@Serializable
data class Meta(
    val district: String,
    val offsetMinutes: Int,
    val qiblaBearing: String,
)

sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>
    data class Err(val code: Int, val message: String) : ApiResult<Nothing>
}
