/* AmSatApiClient.kt - AMSAT official Satellite Status API v1 client (pure JVM).
 * Endpoints (verified 2026-08):
 *   GET https://www.amsat.org/status/api/v1/catalog.php             -> satellite catalog
 *   GET https://www.amsat.org/status/api/v1/reports.php?hours=168    -> recent status reports
 * Report fields: id, name ("SO-50_[FM]"), callsign, report, grid_square, reported_time (ISO 8601 UTC).
 * Status values: Heard / Telemetry Only / Not Heard / Crew Active.
 */
package com.rtbishop.look4sat.core.domain.amsat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** One report from the AMSAT API. */
@Serializable
data class ApiReport(
    val id: String,
    val name: String, // "SO-50_[FM]" (API name, includes the mode suffix)
    val callsign: String,
    val report: String, // Heard / Telemetry Only / Not Heard / Crew Active
    @SerialName("grid_square") val gridSquare: String,
    @SerialName("reported_time") val reportedTimeIso: String
)

/** AMSAT official Satellite Status API v1 client. */
class AmSatApiClient(private val baseUrl: String = "https://www.amsat.org/status/api/v1") {

    private val json = Json { ignoreUnknownKeys = true }

    /** Fetch the full satellite catalog; returns API names (e.g. "SO-50_[FM]"). */
    fun fetchCatalog(): List<String> {
        val body = httpGet("$baseUrl/catalog.php") ?: return emptyList()
        return try {
            val arr = json.parseToJsonElement(body).jsonObject["data"]!!.jsonArray
            arr.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.contentOrNull }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Fetch reports for a rolling UTC window. Empty on failure. */
    fun fetchAllReports(hours: Int = 168): List<ApiReport> {
        val body = httpGet("$baseUrl/reports.php?hours=$hours&limit=500") ?: return emptyList()
        return try {
            json.decodeFromString<ApiEnvelope>(body).data
        } catch (e: Exception) {
            emptyList()
        }
    }
    companion object {
/** Parse "2026-08-05T07:30:00Z" to epoch seconds (minSdk 24: no java.time). */
        fun parseIsoUtcSec(iso: String): Long {
            val m = Regex("""(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})""").find(iso) ?: return 0L
            val (y, mo, d, h, mi, s) = m.destructured
            val days = daysFromCivil(y.toInt(), mo.toInt(), d.toInt())
            return days * 86400L + h.toInt() * 3600L + mi.toInt() * 60L + s.toInt()
        }

/** Days since 1970-01-01 (proleptic Gregorian civil calendar). */
        private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
            val y = if (month <= 2) year - 1 else year
            val era = (if (y >= 0) y else y - 399) / 400
            val yoe = y - era * 400
            val doy = (153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1
            val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
            return era * 146097L + doe - 719468
        }
    }
    private fun httpGet(url: String): String? {
        return try {
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            conn.setRequestProperty("User-Agent", "Look4Sat/4.5.5")
            if (conn.responseCode !in 200..299) {
                conn.disconnect()
                return null
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            text
        } catch (e: Exception) {
            null
        }
    }
}

@Serializable
private data class ApiEnvelope(val data: List<ApiReport> = emptyList())
