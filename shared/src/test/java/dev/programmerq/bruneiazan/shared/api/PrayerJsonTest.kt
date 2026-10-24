package dev.programmerq.bruneiazan.shared.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrayerJsonTest {

    private val dayJson = """
        {
          "status": "success",
          "code": 200,
          "meta": {
            "source": "PDI",
            "timezone": "Asia/Brunei",
            "qibla_bearing": "291.6° WNW",
            "district": "Daerah Brunei-Muara",
            "offset_minutes": 0,
            "format": "24h",
            "generated_at": "2026-09-28T14:45:00+08:00"
          },
          "data": {
            "day": "Isnin",
            "date": "28/09/2026",
            "date_iso": "2026-09-28",
            "date_malay": "28 September 2026",
            "hijri": "16 Rabiulakhir 1448",
            "timings": {"imsak":"04:41","suboh":"04:51","syuruk":"06:08","doha":"06:31","zohor":"12:12","asar":"15:22","maghrib":"18:14","isyak":"19:23"},
            "schedule": [
              {"key":"imsak","name":"Imsak","time":"04:41","time_24":"04:41","time_12":"4:41 AM","type":"TAHAJJUD"},
              {"key":"suboh","name":"Suboh","time":"04:51","time_24":"04:51","time_12":"4:51 AM","type":"FARDHU"},
              {"key":"syuruk","name":"Syuruk","time":"06:08","time_24":"06:08","time_12":"6:08 AM","type":"TERBIT MATAHARI"},
              {"key":"doha","name":"Doha","time":"06:31","time_24":"06:31","time_12":"6:31 AM","type":"SUNAT"},
              {"key":"zohor","name":"Zohor","time":"12:12","time_24":"12:12","time_12":"12:12 PM","type":"FARDHU"},
              {"key":"asar","name":"Asar","time":"15:22","time_24":"15:22","time_12":"3:22 PM","type":"FARDHU"},
              {"key":"maghrib","name":"Maghrib","time":"18:14","time_24":"18:14","time_12":"6:14 PM","type":"FARDHU"},
              {"key":"isyak","name":"Isyak","time":"19:23","time_24":"19:23","time_12":"7:23 PM","type":"FARDHU"}
            ]
          }
        }
    """.trimIndent()

    private val monthJson = """
        {
          "status": "success",
          "code": 200,
          "meta": {"district": "Daerah Brunei-Muara", "offset_minutes": 0, "qibla_bearing": "291.6° WNW"},
          "data": [
            {
              "day": "Sabtu", "date": "17/10/2026", "date_iso": "2026-10-17",
              "date_malay": "17 Oktober 2026", "hijri": "6 Jamadilawal 1448",
              "schedule": [
                {"key":"imsak","name":"Imsak","time":"04:38","time_24":"04:38","time_12":"4:38 AM","type":"TAHAJJUD"},
                {"key":"suboh","name":"Suboh","time":"04:48","time_24":"04:48","time_12":"4:48 AM","type":"FARDHU"}
              ]
            },
            {
              "day": "Ahad", "date": "18/10/2026", "date_iso": "2026-10-18",
              "date_malay": "18 Oktober 2026", "hijri": "7 Jamadilawal 1448",
              "schedule": [
                {"key":"imsak","name":"Imsak","time":"04:38","time_24":"04:38","time_12":"4:38 AM","type":"TAHAJJUD"},
                {"key":"suboh","name":"Suboh","time":"04:48","time_24":"04:48","time_12":"4:48 AM","type":"FARDHU"}
              ]
            }
          ]
        }
    """.trimIndent()

    private val errorJson = """{"status":"error","code":404,"message":"Jadual waktu tidak dijumpai."}"""

    @Test
    fun `parseDay returns meta and 8-item schedule`() {
        val result = PrayerJson.parseDay(dayJson)
        assertTrue(result is ApiResult.Ok)
        val (meta, day) = (result as ApiResult.Ok).value
        assertEquals("Daerah Brunei-Muara", meta.district)
        assertEquals("291.6° WNW", meta.qiblaBearing)
        assertEquals("2026-09-28", day.dateIso)
        assertEquals("Isnin", day.day)
        assertEquals("16 Rabiulakhir 1448", day.hijri)
        assertEquals(8, day.schedule.size)
        assertEquals(
            ScheduleItem("imsak", "Imsak", "04:41", "4:41 AM", "TAHAJJUD"),
            day.schedule[0],
        )
    }

    @Test
    fun `parseMonth returns list of days`() {
        val result = PrayerJson.parseMonth(monthJson)
        assertTrue(result is ApiResult.Ok)
        val days = (result as ApiResult.Ok).value
        assertEquals(2, days.size)
        assertEquals("2026-10-17", days[0].dateIso)
        assertEquals("2026-10-18", days[1].dateIso)
        assertEquals(2, days[0].schedule.size)
    }

    @Test
    fun `error body maps to Err`() {
        val result = PrayerJson.parseDay(errorJson)
        assertTrue(result is ApiResult.Err)
        val err = result as ApiResult.Err
        assertEquals(404, err.code)
        assertEquals("Jadual waktu tidak dijumpai.", err.message)
    }

    @Test
    fun `garbage body maps to Err`() {
        val result = PrayerJson.parseDay("not json at all")
        assertTrue(result is ApiResult.Err)
    }
}
