package com.rtbishop.look4sat.core.data.repository

import com.rtbishop.look4sat.core.domain.amsat.AmSatApiClient
import com.rtbishop.look4sat.core.domain.amsat.ApiReport
import com.rtbishop.look4sat.core.domain.model.SatDay
import com.rtbishop.look4sat.core.domain.model.SatReport
import com.rtbishop.look4sat.core.domain.model.SatSlot
import com.rtbishop.look4sat.core.domain.model.SatStatus
import com.rtbishop.look4sat.core.domain.model.SatStatusPage
import com.rtbishop.look4sat.core.domain.repository.IAmSatRepository
import java.util.Calendar
import java.util.TimeZone

/** AMSAT status repository: official API v1 -> SatStatusPage (replaces the HTML parser). */
class AmSatRepository(private val apiClient: AmSatApiClient) : IAmSatRepository {

    override suspend fun fetchStatus(): SatStatusPage? {
        val nowSec = System.currentTimeMillis() / 1000
        val names = apiClient.fetchCatalog()
        val reports = apiClient.fetchAllReports(hours = 168)
        if (names.isEmpty() && reports.isEmpty()) return null
        val statuses = buildStatuses(names, reports, nowSec)
        val reportMap = reports.associate { it.id to toSatReport(it) }
        return SatStatusPage(System.currentTimeMillis(), statuses, reportMap)
    }

    /** Build one SatStatus (6 days x 12 slots) per catalog satellite, slotting reports by age. */
    private fun buildStatuses(names: List<String>, reports: List<ApiReport>, nowSec: Long): List<SatStatus> {
        val byName = reports.groupBy { it.name }
        val monthAbbr = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val labels = (0 until 6).map { d ->
            utc.timeInMillis = (nowSec - d * 86400L) * 1000
            "${monthAbbr[utc.get(Calendar.MONTH)]} ${utc.get(Calendar.DAY_OF_MONTH)}"
        }
        return names.map { name ->
            val slots = (0 until 72).map { slotIdx ->
                val slotStart = nowSec - (slotIdx + 1) * 7200L
                val slotEnd = nowSec - slotIdx * 7200L
                val inSlot = byName[name].orEmpty().filter { it.reportedTimeUtcSec() in slotStart until slotEnd }
                if (inSlot.isEmpty()) {
                    SatSlot(statusColor = NoReportGray, count = 0)
                } else {
                    val newest = inSlot.maxByOrNull { it.reportedTimeUtcSec() }!!
                    SatSlot(
                        statusColor = statusColorOf(newest.report),
                        count = inSlot.size,
                        reportIds = inSlot.map { it.id }
                    )
                }
            }
            val days = (0 until 6).map { d ->
                SatDay(dateLabel = labels[d], slots = slots.subList(d * 12, (d + 1) * 12))
            }
            SatStatus(name = name, days = days)
        }
    }

    private fun toSatReport(r: ApiReport): SatReport {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = r.reportedTimeUtcSec() * 1000
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

    private fun statusColorOf(report: String): Long = when (report.lowercase()) {
        "heard", "crew active" -> ActiveBlue
        "telemetry only" -> TlmOrange
        "not heard" -> NotHeardPink
        else -> ConflictDeepOrange
    }

    companion object {
        private const val ActiveBlue = 0xFF648FFF
        private const val TlmOrange = 0xFFFFB000
        private const val NotHeardPink = 0xFFDC267F
        private const val ConflictDeepOrange = 0xFFFE6100
        private const val NoReportGray = 0xFFC0C0C0
    }
}

private fun ApiReport.reportedTimeUtcSec(): Long =
    AmSatApiClient.parseIsoUtcSec(reportedTimeIso)
