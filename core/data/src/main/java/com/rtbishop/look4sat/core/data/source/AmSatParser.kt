package com.rtbishop.look4sat.core.data.source

import com.rtbishop.look4sat.core.domain.model.SatDay
import com.rtbishop.look4sat.core.domain.model.SatReport
import com.rtbishop.look4sat.core.domain.model.SatSlot
import com.rtbishop.look4sat.core.domain.model.SatStatus
import com.rtbishop.look4sat.core.domain.model.SatStatusPage
import java.util.regex.Pattern

/**
 * AMSAT 卫星状态页解析器(https://amsat.org/status/)
 *
 * 页面结构(静态 HTML, 2026-08 实测):
 * - 状态表: <table> 内 48 行, 表头 = Name + 6 天(每个 colspan=12)
 *   数据行 73 格: [0]=卫星名, [1..72] = 6 天 × 12 个 2 小时槽(新→旧)
 *   每格 <td width=9 bgcolor="颜色">数字</td>, 有报告时数字带
 *   docTips.show('id') 链接
 * - 报告详情在页面内嵌 JS:
 *   tips.a885153 = new Array(5,5,120,'状态<br>呼号<br>网格<br>日期<br>时间段 UTC')
 *
 * 状态色: #648fff=活跃, #ffb000=仅遥测, #dc267f=未听到, #fe6100=矛盾
 */
object AmSatParser {

    private val STATUS_COLORS = mapOf(
        "#648fff" to 0xFF648FFF.toLong(),
        "#ffb000" to 0xFFFFB000.toLong(),
        "#dc267f" to 0xFFDC267F.toLong(),
        "#fe6100" to 0xFFFE6100.toLong()
    )
    private val GRAY = 0xFFC0C0C0.toLong()

    private val rowRe = Pattern.compile("<tr>(.*?)</tr>", Pattern.DOTALL)
    private val cellRe = Pattern.compile("(<t[dh][^>]*>.*?</t[dh]>)", Pattern.DOTALL)
    private val bgRe = Pattern.compile("bgcolor=\"?(#[0-9a-fA-F]{6}|C0C0C0)\"?")
    private val linkRe = Pattern.compile("docTips\\.show\\('(a\\d+)'\\)")
    private val tipRe = Pattern.compile(
        "tips\\.(a\\d+)\\s*=\\s*new\\s+Array\\(\\s*\\d+,\\s*\\d+,\\s*\\d+,\\s*'([^']*)'"
    )

    /** 解析完整页面 */
    fun parse(html: String, fetchedAtUtcMs: Long): SatStatusPage {
        val reports = parseReports(html)
        val statuses = parseStatusTable(html, reports)
        return SatStatusPage(fetchedAtUtcMs, statuses, reports)
    }

    /** 提取全部报告(tips.* JS 数组) */
    fun parseReports(html: String): Map<String, SatReport> {
        val map = mutableMapOf<String, SatReport>()
        val m = tipRe.matcher(html)
        while (m.find()) {
            val id = m.group(1)
            val parts = m.group(2).split("<br>")
            map[id] = SatReport(
                id = id,
                statusText = parts.getOrElse(0) { "" }.trim(),
                call = parts.getOrElse(1) { "" }.trim(),
                grid = parts.getOrElse(2) { "" }.trim(),
                dateUtc = parts.getOrElse(3) { "" }.trim(),
                timeUtc = parts.getOrElse(4) { "" }.trim()
            )
        }
        return map
    }

    /** 解析状态表(48 行卫星) */
    fun parseStatusTable(html: String, reports: Map<String, SatReport>): List<SatStatus> {
        val result = mutableListOf<SatStatus>()
        val tables = extractTables(html)
        for (table in tables) {
            val rows = rowRe.matcher(table)
            val parsed = mutableListOf<SatStatus>()
            var rowIndex = 0
            var dayHeaders: List<String> = emptyList()
            while (rows.find()) {
                val rowHtml = rows.group(1)
                val cells = cellRe.matcher(rowHtml)
                val cellList = mutableListOf<String>()
                while (cells.find()) cellList.add(cells.group(1))
                if (rowIndex == 0) {
                    // 表头: Name + 6 天(每个 colspan=12)
                    dayHeaders = cellList.drop(1).take(6).map { stripHtml(it) }
                    rowIndex++
                    continue
                }
                if (cellList.size < 7) { rowIndex++; continue }
                val name = stripHtml(cellList[0])
                if (name.isBlank()) { rowIndex++; continue }
                val days = mutableListOf<SatDay>()
                for (d in 0 until 6) {
                    val start = 1 + d * 12
                    val end = start + 12
                    val slots = (start until end).mapNotNull { i ->
                        cellList.getOrNull(i)?.let { cell ->
                            val bg = bgRe.matcher(cell)
                            val color = if (bg.find()) {
                                STATUS_COLORS[bg.group(1).lowercase()] ?: GRAY
                            } else GRAY
                            val link = linkRe.matcher(cell)
                            val ids = mutableListOf<String>()
                            while (link.find()) ids.add(link.group(1))
                            val hasReport = ids.isNotEmpty()
                            val count = if (hasReport) {
                                stripHtml(cell).trim().toIntOrNull() ?: ids.size
                            } else 0
                            SatSlot(
                                statusColor = if (hasReport) color else GRAY,
                                count = count,
                                reportIds = ids
                            )
                        }
                    }
                    days.add(SatDay(dayHeaders.getOrElse(d) { "" }, slots))
                }
                parsed.add(SatStatus(name, days))
                rowIndex++
            }
            if (parsed.isNotEmpty()) {
                result.addAll(parsed)
                break
            }
        }
        return result
    }

    private fun extractTables(html: String): List<String> {
        val result = mutableListOf<String>()
        val m = Pattern.compile("<table.*?</table>", Pattern.DOTALL).matcher(html)
        while (m.find()) result.add(m.group())
        return result
    }

    private fun stripHtml(s: String): String =
        s.replace(Regex("<[^>]+>"), "").trim()
}
