package com.rtbishop.look4sat.core.data.repository

import com.rtbishop.look4sat.core.domain.model.SatDay
import com.rtbishop.look4sat.core.domain.model.SatReport
import com.rtbishop.look4sat.core.domain.model.SatSlot
import com.rtbishop.look4sat.core.domain.model.SatStatus
import com.rtbishop.look4sat.core.domain.model.SatStatusPage
import com.rtbishop.look4sat.core.domain.repository.IAmSatRepository
import com.rtbishop.look4sat.core.domain.source.IRemoteSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** One report from the AMSAT API (data layer model). */
private data class ApiReport(
    val id: String,
    val name: String,
    val callsign: String,
    val report: String,
    val gridSquare: String,
    val reportedTimeUtcSec: Long
)

/** AMSAT status repository using RemoteSource (Clean Architecture: data layer handles HTTP). */
class AmSatRepository(private val remoteSource: IRemoteSource) : IAmSatRepository {

    private val isoUtcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override suspend fun fetchStatus(): SatStatusPage? = withContext(Dispatchers.IO) {
        val nowSec = System.currentTimeMillis() / 1000
        val catalogJson = remoteSource.getAmSatCatalog() ?: return@withContext null
        // 72h = 3 days; API hard cap is limit=500 regardless of what we send.
        // 500 records across ~100 catalog satellites ≈ ~1-5 reports/satellite/day — enough for 3 days.
        // Upgrade path: paginate or request AMSAT to raise the cap if catalog grows beyond ~200 sats.
        val reportsJson = remoteSource.getAmSatReports(hours = 72, limit = 500) ?: return@withContext null
        val names = parseCatalog(catalogJson)
        val reports = parseReports(reportsJson)

        if (names.isEmpty() && reports.isEmpty()) return@withContext null

        val statuses = buildStatuses(names, reports, nowSec)
        val reportMap = reports.associate { it.id to toSatReport(it) }
        SatStatusPage(System.currentTimeMillis(), statuses, reportMap)
    }

    /** Parse catalog JSON to list of satellite names */
    private fun parseCatalog(json: String): List<String> {
        return try {
            val arr = JSONObject(json).getJSONArray("data")
            (0 until arr.length()).map { arr.getJSONObject(it).getString("name") }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Parse reports JSON to list of ApiReport domain objects */
    private fun parseReports(json: String): List<ApiReport> {
        return try {
            val arr = JSONObject(json).getJSONArray("data")
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val iso = o.optString("reported_time", "")
                if (iso.isEmpty()) null else ApiReport(
                    id = o.optString("id", ""),
                    name = o.optString("name", ""),
                    callsign = o.optString("callsign", ""),
                    report = o.optString("report", ""),
                    gridSquare = o.optString("grid_square", ""),
                    reportedTimeUtcSec = parseIsoUtcSec(iso)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Parse ISO 8601 UTC timestamp to epoch seconds (e.g., "2026-08-05T07:30:00Z") */
    private fun parseIsoUtcSec(iso: String): Long {
        return try {
            (isoUtcFormat.parse(iso)?.time ?: 0L) / 1000
        } catch (_: Exception) {
            0L
        }
    }

    /** Build one SatStatus (5 days x 12 slots) per catalog satellite, slotting reports by age. */
    private fun buildStatuses(names: List<String>, reports: List<ApiReport>, nowSec: Long): List<SatStatus> {
        val byName = reports.groupBy { it.name }
        val monthAbbr = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val labels = (0 until 3).map { d ->
            utc.timeInMillis = (nowSec - d * 86400L) * 1000
            "${monthAbbr[utc.get(Calendar.MONTH)]} ${utc.get(Calendar.DAY_OF_MONTH)}"
        }
        return names.map { name ->
            val slots = (0 until 36).map { slotIdx ->
                val slotStart = nowSec - (slotIdx + 1) * 7200L
                val slotEnd = nowSec - slotIdx * 7200L
                val inSlot = byName[name].orEmpty().filter { it.reportedTimeUtcSec in slotStart until slotEnd }
                if (inSlot.isEmpty()) {
                    SatSlot(statusColor = NO_REPORT_GRAY, count = 0)
                } else {
                    val newest = inSlot.maxByOrNull { it.reportedTimeUtcSec }!!
                    SatSlot(
                        statusColor = statusColorOf(newest.report),
                        count = inSlot.size,
                        reportIds = inSlot.map { it.id }
                    )
                }
            }
            val days = (0 until 3).map { d ->
                SatDay(dateLabel = labels[d], slots = slots.subList(d * 12, (d + 1) * 12))
            }
            SatStatus(name = name, days = days)
        }
    }

    private fun toSatReport(r: ApiReport): SatReport {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = r.reportedTimeUtcSec * 1000
        val hh = cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val mm = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
        val y = cal.get(Calendar.YEAR)
        val mo = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val d = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        return SatReport(
            id = r.id,
            statusText = r.report,
            call = r.callsign,
            grid = r.gridSquare,
            dateUtc = "$y-$mo-$d",
            timeUtc = "$hh:$mm UTC"
        )
    }

    /** Map status text to color value (for UI rendering). */
    private fun statusColorOf(report: String): Long = when (report.lowercase()) {
        "heard", "crew active" -> ACTIVE_BLUE
        "telemetry only" -> TLM_ORANGE
        "not heard" -> NOT_HEARD_PINK
        else -> CONFLICT_DEEP_ORANGE
    }

    companion object {
        // AMSAT official status colors (from amsat.org/status)
        private const val ACTIVE_BLUE = 0xFF648FFF
        private const val TLM_ORANGE = 0xFFFFB000
        private const val NOT_HEARD_PINK = 0xFFDC267F
        private const val CONFLICT_DEEP_ORANGE = 0xFFFE6100
        private const val NO_REPORT_GRAY = 0xFFC0C0C0
    }
}
