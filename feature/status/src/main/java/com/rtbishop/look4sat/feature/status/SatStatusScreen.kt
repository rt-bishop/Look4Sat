package com.rtbishop.look4sat.feature.status

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rtbishop.look4sat.core.domain.model.SatDay
import com.rtbishop.look4sat.core.domain.model.SatReport
import com.rtbishop.look4sat.core.domain.model.SatSlot
import com.rtbishop.look4sat.core.domain.model.SatStatus
import com.rtbishop.look4sat.core.domain.repository.IContainerProvider
import com.rtbishop.look4sat.core.presentation.InfoDialog
import com.rtbishop.look4sat.core.presentation.R
import com.rtbishop.look4sat.core.presentation.layoutPadding
import java.util.Calendar

/** Fixed width per day tile — tablet-safe; name column absorbs remaining space. */
private val TILE_WIDTH: Dp = 64.dp

/**
 * Map AMSAT status text to Material3 colorScheme colors.
 * Addresses PR #233 review: use colorScheme instead of hardcoded Color() constants.
 */
@Composable
private fun statusColorOf(statusText: String): Color {
    return when {
        statusText.contains("Heard", ignoreCase = true) && !statusText.contains("Not", ignoreCase = true) ->
            MaterialTheme.colorScheme.tertiary
        statusText.contains("Telemetry", ignoreCase = true) || statusText.contains("Beacon", ignoreCase = true) ->
            MaterialTheme.colorScheme.tertiaryContainer
        statusText.contains("Not Heard", ignoreCase = true) ->
            Color(0xFFDC267F)
        else ->
            MaterialTheme.colorScheme.error
    }
}

@Composable
fun SatStatusDestination() {
    val context = LocalContext.current
    val container = (context.applicationContext as IContainerProvider).getMainContainer()
    val viewModel: SatStatusViewModel = viewModel(factory = SatStatusViewModel.factory(container))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SatStatusScreen(uiState) { viewModel.refresh() }
}

@Composable
private fun SatStatusScreen(uiState: SatStatusUiState, refresh: () -> Unit) {
    var selectedDay by remember { mutableStateOf<Pair<SatStatus, SatDay>?>(null) }
    Column(modifier = Modifier.fillMaxSize().layoutPadding()) {
        StatusHeader(fetchedAtUtcMs = uiState.fetchedAtUtcMs, isRefreshing = uiState.isRefreshing, onRefresh = refresh)
        LegendRow()

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null && uiState.statuses.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = stringResource(id = R.string.amsat_load_failed), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = refresh) { Text(text = stringResource(id = R.string.amsat_retry)) }
                }
            }
            else -> {
                HeaderRow(statuses = uiState.statuses)
                HorizontalDivider(thickness = 1.dp)
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.statuses, key = { it.name }) { status ->
                        StatusRow(status = status, onClickDay = { day -> selectedDay = status to day })
                    }
                }
            }
        }
    }

    selectedDay?.let { (status, day) ->
        ReportDialog(statusName = status.name, day = day, reports = uiState.reports, onDismiss = { selectedDay = null })
    }
}

/** Top: update time + refresh button (spinner while loading) */
@Composable
private fun StatusHeader(fetchedAtUtcMs: Long, isRefreshing: Boolean, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (fetchedAtUtcMs > 0)
                stringResource(id = R.string.amsat_updated) + " " + formatFetchedAt(fetchedAtUtcMs)
            else
                stringResource(id = R.string.amsat_title),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onRefresh),
            contentAlignment = Alignment.Center
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_refresh),
                    contentDescription = stringResource(id = R.string.amsat_refresh),
                    modifier = Modifier.size(16.dp),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

/** Legend: FlowRow of colored chips — wraps to two lines on narrow screens, stays one line when wide. */
@Composable
private fun LegendRow() {
    val legend = listOf(
        stringResource(id = R.string.amsat_active) to Color(0xFF648FFF),
        stringResource(id = R.string.amsat_tlm) to Color(0xFFFFB000),
        stringResource(id = R.string.amsat_not_heard) to Color(0xFFDC267F),
        stringResource(id = R.string.amsat_conflict) to Color(0xFFFE6100)
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        legend.forEach { (label, color) ->
            val alphaColor = color.copy(alpha = 0.25f)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(alphaColor)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Header: satellite name column + fixed-width date labels aligned to tiles. */
@Composable
private fun HeaderRow(statuses: List<SatStatus>) {
    val dates = statuses.firstOrNull()?.days?.map { it.dateLabel } ?: emptyList()
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.amsat_name),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f).padding(start = 4.dp)
        )
        dates.forEach { date ->
            Text(
                text = date,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(TILE_WIDTH)
            )
        }
    }
}

/** Satellite row: name takes remaining width; day tiles are fixed-width (tablet-safe). */
@Composable
private fun StatusRow(status: SatStatus, onClickDay: (SatDay) -> Unit) {
    val noReportGray = 0xFFC0C0C0L
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = status.name,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 4.dp)
        )
        status.days.forEach { day ->
            val slot = day.slots.firstOrNull { it.statusColor != noReportGray } ?: day.slots.first()
            DayCell(
                slot = slot,
                modifier = Modifier.width(TILE_WIDTH).padding(horizontal = 2.dp),
                onClick = { onClickDay(day) }
            )
        }
    }
}

/** Day block: newest reported status among the day's 12 slots; gray when none. */
@Composable
private fun DayCell(slot: SatSlot, modifier: Modifier, onClick: () -> Unit) {
    val color = Color(slot.statusColor)
    Box(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (slot.count > 0) {
            Text(text = slot.count.toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

/** Report detail dialog (callsign / date / time / grid) */
@Composable
private fun ReportDialog(statusName: String, day: SatDay, reports: Map<String, SatReport>, onDismiss: () -> Unit) {
    val dayReports = day.slots.flatMap { it.reportIds }.mapNotNull { reports[it] }
    InfoDialog(title = "$statusName · ${day.dateLabel}", onDismiss = onDismiss, onAccept = onDismiss) {
        if (dayReports.isEmpty()) {
            Text(stringResource(id = R.string.amsat_no_reports))
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                items(dayReports) { report ->
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColorOf(report.statusText))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = report.statusText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "${report.call}  ${report.dateUtc}  ${report.timeUtc}" +
                                if (report.grid.isNotBlank() && report.grid != "-") "  ${report.grid}" else "",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}

private val MONTH_ABBR = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun formatFetchedAt(utcMs: Long): String {
    val cal = Calendar.getInstance()
    cal.timeInMillis = utcMs
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val month = MONTH_ABBR[cal.get(Calendar.MONTH)]
    val year = cal.get(Calendar.YEAR)
    val hh = cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val mm = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
    val ss = cal.get(Calendar.SECOND).toString().padStart(2, '0')
    return "$day$month $year - $hh:$mm:$ss"
}
