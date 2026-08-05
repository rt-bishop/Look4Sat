package com.rtbishop.look4sat.core.domain.model

/** 单条卫星状态报告(AMSAT 网站 tooltip 数据) */
data class SatReport(
    val id: String,          // 报告 ID(a885153)
    val statusText: String,  // Heard / Telemetry Only / Not Heard ...
    val call: String,        // 呼号
    val grid: String,        // 网格坐标(可为空)
    val dateUtc: String,     // 2026-08-04
    val timeUtc: String      // 2:46-:59 UTC
)

/** 单个 2 小时槽的状态 */
data class SatSlot(
    val statusColor: Long,   // ARGB 状态色(-1 = 无报告)
    val count: Int,          // 报告数量(0 = 无)
    val reportIds: List<String> = emptyList() // 该槽报告 ID 列表
)

/** 卫星一天的状态(12 个 2 小时槽) */
data class SatDay(
    val dateLabel: String,   // "Aug 4"
    val slots: List<SatSlot> // 12 槽(00-02 ... 22-24)
)

/** 单个卫星 6 天状态 */
data class SatStatus(
    val name: String,        // "AO-123_[FM]"
    val days: List<SatDay>   // 6 天(新→旧)
)

/** 页面整体解析结果 */
data class SatStatusPage(
    val fetchedAtUtcMs: Long,
    val statuses: List<SatStatus>,
    val reports: Map<String, SatReport> // id → 报告
)
