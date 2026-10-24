package dev.programmerq.bruneiazan.shared

import dev.programmerq.bruneiazan.shared.api.ApiResult
import dev.programmerq.bruneiazan.shared.api.MonthApi
import dev.programmerq.bruneiazan.shared.api.PrayerDay
import dev.programmerq.bruneiazan.shared.api.ScheduleItem
import dev.programmerq.bruneiazan.shared.schedule.MonthCache
import dev.programmerq.bruneiazan.shared.schedule.PrayerRepository
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MonthCacheTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun day(dateIso: String) = PrayerDay(
        day = "X", dateIso = dateIso, dateMalay = "x", hijri = "x",
        schedule = listOf(ScheduleItem("suboh", "Suboh", "04:47", "4:47 AM", "FARDHU")),
    )

    private fun cache() = MonthCache(tmp.newFolder("months"))

    @Test
    fun `put then get round-trips days`() {
        val c = cache()
        val days = listOf(day("2026-10-17"), day("2026-10-18"))
        c.put("brunei-muara", YearMonth.of(2026, 10), days)
        assertEquals(days, c.get("brunei-muara", YearMonth.of(2026, 10)))
    }

    @Test
    fun `missing month returns null`() {
        assertNull(cache().get("brunei-muara", YearMonth.of(2026, 10)))
    }

    @Test
    fun `districts do not collide`() {
        val c = cache()
        c.put("tutong", YearMonth.of(2026, 10), listOf(day("2026-10-17")))
        assertNull(c.get("belait", YearMonth.of(2026, 10)))
    }

    @Test
    fun `day convenience looks up by dateIso`() {
        val c = cache()
        c.put("brunei-muara", YearMonth.of(2026, 10), listOf(day("2026-10-17")))
        assertEquals("2026-10-17", c.day("brunei-muara", "2026-10-17")?.dateIso)
        assertNull(c.day("brunei-muara", "2026-10-20"))
    }

    @Test
    fun `repository returns cached day without fetching`() = runBlocking {
        var fetches = 0
        val api = MonthApi { _, _, _ -> fetches++; ApiResult.Ok(emptyList()) }
        val c = cache()
        c.put("brunei-muara", YearMonth.of(2026, 10), listOf(day("2026-10-24")))
        val repo = PrayerRepository(c, api)
        assertEquals("2026-10-24", repo.day(LocalDate.of(2026, 10, 24), "brunei-muara")?.dateIso)
        assertEquals(0, fetches)
    }

    @Test
    fun `repository fetches and caches on miss`() = runBlocking {
        val fetched = mutableListOf<Pair<Int, Int>>()
        val api = MonthApi { y, m, _ ->
            fetched += y to m
            ApiResult.Ok(listOf(day("2026-10-24")))
        }
        val repo = PrayerRepository(cache(), api)
        assertEquals("2026-10-24", repo.day(LocalDate.of(2026, 10, 24), "brunei-muara")?.dateIso)
        assertEquals(listOf(2026 to 10), fetched)
    }

    @Test
    fun `repository fetches the month containing the requested date`() = runBlocking {
        val fetched = mutableListOf<Pair<Int, Int>>()
        val api = MonthApi { y, m, _ ->
            fetched += y to m
            ApiResult.Ok(listOf(day("2026-11-01")))
        }
        val repo = PrayerRepository(cache(), api)
        assertEquals("2026-11-01", repo.day(LocalDate.of(2026, 11, 1), "brunei-muara")?.dateIso)
        assertEquals(listOf(2026 to 11), fetched)
    }

    @Test
    fun `repository returns null when date absent from fetched month`() = runBlocking {
        val api = MonthApi { _, _, _ -> ApiResult.Ok(listOf(day("2026-10-17"))) }
        val repo = PrayerRepository(cache(), api)
        assertNull(repo.day(LocalDate.of(2026, 10, 30), "brunei-muara"))
    }

    @Test
    fun `repository returns null on api error`() = runBlocking {
        val api = MonthApi { _, _, _ -> ApiResult.Err(404, "not found") }
        val repo = PrayerRepository(cache(), api)
        assertNull(repo.day(LocalDate.of(2026, 10, 24), "brunei-muara"))
    }
}
