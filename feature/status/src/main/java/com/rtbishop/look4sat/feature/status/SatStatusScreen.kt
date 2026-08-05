package com.rtbishop.look4sat.feature.status

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rtbishop.look4sat.core.domain.model.SatDay
import com.rtbishop.look4sat.core.domain.model.SatReport
import com.rtbishop.look4sat.core.domain.model.SatSlot
import com.rtbishop.look4sat.core.domain.model.SatStatus
import com.rtbishop.look4sat.core.domain.repository.IMainContainer
import com.rtbishop.look4sat.core.presentation.R
import java.util.Calendar

// ========== 官网状态色(amsat.org/status 原色) ==========
val ActiveBlue = Color(0xFF648FFF)
val TlmOrange = Color(0xFFFFB000)
val NotHeardPink = Color(0xFFDC267F)
val ConflictDeepOrange = Color(0xFFFE6100)
val NoReportGray = Color(0xFFC0C0C0)

@Composable
fun SatStatusScreen(container: IMainContainer) {
    val viewModel: SatStatusViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SatStatusViewModel(container) }
        }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedDay by remember { mutableStateOf<Pair<SatStatus, SatDay>?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        // 顶部: 图例 + 刷新
        StatusHeader(
            fetchedAtUtcMs = uiState.fetchedAtUtcMs,
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() }
        )
        LegendRow()

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null && uiState.statuses.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(id = R.string.amsat_load_failed),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> {
                // 表头
                HeaderRow(statuses = uiState.statuses)
                HorizontalDivider(thickness = 1.dp)
                // 行
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.statuses, key = { it.name }) { status ->
                        StatusRow(
                            status = status,
                            onClickDay = { day -> selectedDay = status to day }
                        )
                    }
                }
            }
        }
    }

    selectedDay?.let { (status, day) ->
        ReportDialog(
            statusName = status.name,
            day = day,
            reports = uiState.reports,
            onDismiss = { selectedDay = null }
        )
    }
}

/** 顶部: 更新时间 + 刷新按钮(加载中旋转动画) */
@Composable
private fun StatusHeader(
    fetchedAtUtcMs: Long,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "refresh")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Restart),
        label = "angle"
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (fetchedAtUtcMs > 0) stringResource(id = R.string.amsat_updated) + " " + formatFetchedAt(fetchedAtUtcMs) else stringResource(id = R.string.amsat_title),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onRefresh),
            contentAlignment = Alignment.Center
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    text = "↻",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.rotate(angle)
                )
            }
        }
    }
}

/** 图例(官网四色) */
@Composable
private fun LegendRow() {
    val legend = listOf(
        stringResource(id = R.string.amsat_active) to ActiveBlue,
        stringResource(id = R.string.amsat_tlm) to TlmOrange,
        stringResource(id = R.string.amsat_not_heard) to NotHeardPink,
        stringResource(id = R.string.amsat_conflict) to ConflictDeepOrange
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        legend.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** 表头: 卫星名 + 6 天日期 */
@Composable
private fun HeaderRow(statuses: List<SatStatus>) {
    val dates = statuses.firstOrNull()?.days?.map { it.dateLabel } ?: emptyList()
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.amsat_name),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(2f).padding(start = 4.dp)
        )
        dates.forEach { date ->
            Text(
                text = date,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(0.8f)
            )
        }
    }
}

/** 卫星行: 名称 + 6 天色块(官网色 + 报告数) */
@Composable
private fun StatusRow(status: SatStatus, onClickDay: (SatDay) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = status.name,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(2f).padding(start = 4.dp)
        )
        status.days.forEach { day ->
            val slot = day.slots.firstOrNull { it.statusColor != NoReportGray.value.toInt().toLong() } ?: day.slots.first()
            DayCell(
                slot = slot,
                modifier = Modifier.weight(0.8f).padding(horizontal = 1.dp),
                onClick = { onClickDay(day) }
            )
        }
    }
}

/** 单天色块: 取当天 12 槽中最新的有报告状态; 无报告则灰 */
@Composable
private fun DayCell(slot: SatSlot, modifier: Modifier, onClick: () -> Unit) {
    val color = Color(slot.statusColor)
    Box(
        modifier = modifier
            .height(24.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (slot.count > 0) {
            Text(
                text = slot.count.toString(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/** 报告详情弹窗(3 级: 呼号/日期/时间/网格) */
@Composable
private fun ReportDialog(
    statusName: String,
    day: SatDay,
    reports: Map<String, SatReport>,
    onDismiss: () -> Unit
) {
    val dayReports = day.slots.flatMap { it.reportIds }
        .mapNotNull { reports[it] }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "$statusName · ${day.dateLabel}") },
        text = {
            if (dayReports.isEmpty()) {
                Text(stringResource(id = R.string.amsat_no_reports))
            } else {
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    items(dayReports) { report ->
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(statusColorOf(report.statusText))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = report.statusText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${report.call}  ${report.dateUtc}  ${report.timeUtc}" +
                                    if (report.grid.isNotBlank() && report.grid != "-") "  ${report.grid}" else "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.amsat_close)) }
        }
    )
}

private fun statusColorOf(statusText: String): Color = when {
    statusText.contains("Heard", ignoreCase = true) && !statusText.contains("Not", ignoreCase = true) -> ActiveBlue
    statusText.contains("Telemetry", ignoreCase = true) || statusText.contains("Beacon", ignoreCase = true) -> TlmOrange
    statusText.contains("Not Heard", ignoreCase = true) -> NotHeardPink
    else -> ConflictDeepOrange
}

private fun formatFetchedAt(utcMs: Long): String {
    val cal = Calendar.getInstance()
    cal.timeInMillis = utcMs
    return "%02d-%02d %02d:%02d".format(
        cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH),
        cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)
    )
}
