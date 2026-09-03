package com.rtbishop.look4sat.feature.status

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.rtbishop.look4sat.core.presentation.CardButton
import com.rtbishop.look4sat.core.presentation.InfoDialog
import com.rtbishop.look4sat.core.presentation.R
import com.rtbishop.look4sat.core.presentation.layoutPadding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Fixed width per day tile — tablet-safe; name column absorbs remaining space. */
private val TILE_WIDTH: Dp = 64.dp

private data class UploadOption(
    val apiValue: String,
    val labelResId: Int,
    val color: Color
)

private val UPLOAD_OPTIONS = listOf(
    UploadOption(AMSAT_REPORT_HEARD, R.string.amsat_upload_active, Color(0xFF648FFF)),
    UploadOption(AMSAT_REPORT_TELEMETRY_ONLY, R.string.amsat_upload_tlm, Color(0xFFFFB000)),
    UploadOption(AMSAT_REPORT_NOT_HEARD, R.string.amsat_upload_not_heard, Color(0xFFDC267F)),
    UploadOption(AMSAT_REPORT_CREW_ACTIVE, R.string.amsat_upload_crew_active, Color(0xFFFE6100))
)

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

/** Localize AMSAT API status text for the current locale; falls back to the raw text. */
@Composable
private fun statusTextOf(statusText: String): String {
    if (Locale.getDefault().language != Locale.CHINESE.language) return statusText
    return when (statusText.lowercase()) {
        "heard" -> stringResource(R.string.amsat_status_heard)
        "telemetry only" -> stringResource(R.string.amsat_status_tlm_only)
        "not heard" -> stringResource(R.string.amsat_status_not_heard)
        "crew active" -> stringResource(R.string.amsat_status_crew_active)
        else -> statusText
    }
}

@Composable
fun SatStatusDestination() {
    val context = LocalContext.current
    val container = (context.applicationContext as IContainerProvider).getMainContainer()
    val viewModel: SatStatusViewModel = viewModel(factory = SatStatusViewModel.factory(container))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SatStatusScreen(
        uiState = uiState,
        refresh = viewModel::refresh,
        onToggleUpload = viewModel::toggleUploadPanel,
        onUploadReportChange = viewModel::setUploadReport,
        onUploadCallsignChange = viewModel::setUploadCallsign,
        onUploadGridChange = viewModel::setUploadGrid,
        onRequestSubmitUpload = viewModel::requestSubmitConfirmation,
        onConfirmSubmitUpload = viewModel::confirmSubmitReport,
        onDismissSubmitConfirmation = viewModel::dismissSubmitConfirmation,
        onDismissUpload = viewModel::collapseUploadPanel
    )
}

@Composable
private fun SatStatusScreen(
    uiState: SatStatusUiState,
    refresh: () -> Unit,
    onToggleUpload: () -> Unit,
    onUploadReportChange: (String) -> Unit,
    onUploadCallsignChange: (String) -> Unit,
    onUploadGridChange: (String) -> Unit,
    onRequestSubmitUpload: () -> Unit,
    onConfirmSubmitUpload: (String) -> Unit,
    onDismissSubmitConfirmation: () -> Unit,
    onDismissUpload: () -> Unit
) {
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
        ReportDialog(
            statusName = status.name,
            day = day,
            reports = uiState.reports,
            upload = uiState.upload,
            onToggleUpload = onToggleUpload,
            onReportChange = onUploadReportChange,
            onCallsignChange = onUploadCallsignChange,
            onGridChange = onUploadGridChange,
            onRequestSubmitReport = onRequestSubmitUpload,
            onConfirmSubmitReport = { onConfirmSubmitUpload(status.name) },
            onDismissSubmitConfirmation = onDismissSubmitConfirmation,
            onDismiss = {
                selectedDay = null
                onDismissUpload()
            }
        )
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

/** Report detail dialog (callsign / date / time / grid) + AMSAT report upload panel. */
@Composable
private fun ReportDialog(
    statusName: String,
    day: SatDay,
    reports: Map<String, SatReport>,
    upload: AmSatUploadUiState,
    onToggleUpload: () -> Unit,
    onReportChange: (String) -> Unit,
    onCallsignChange: (String) -> Unit,
    onGridChange: (String) -> Unit,
    onRequestSubmitReport: () -> Unit,
    onConfirmSubmitReport: () -> Unit,
    onDismissSubmitConfirmation: () -> Unit,
    onDismiss: () -> Unit
) {
    val dayReports = day.slots.flatMap { it.reportIds }.mapNotNull { reports[it] }
    InfoDialog(
        title = "$statusName · ${day.dateLabel}",
        onDismiss = onDismiss,
        onAccept = onDismiss,
        extraAction = {
            CardButton(
                onClick = onToggleUpload,
                text = stringResource(if (upload.isExpanded) R.string.amsat_upload_back else R.string.amsat_upload)
            )
        }
    ) {
        AnimatedVisibility(visible = upload.isExpanded) {
            AmSatUploadPanel(
                statusName = statusName,
                upload = upload,
                onReportChange = onReportChange,
                onCallsignChange = onCallsignChange,
                onGridChange = onGridChange,
                onRequestSubmitReport = onRequestSubmitReport
            )
        }
        if (upload.isConfirmingSubmit) {
            AmSatUploadConfirmDialog(
                statusName = statusName,
                upload = upload,
                onConfirm = onConfirmSubmitReport,
                onDismiss = onDismissSubmitConfirmation
            )
        }
        if (dayReports.isEmpty()) {
            Text(
                text = stringResource(id = R.string.amsat_no_reports),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
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
                            Text(text = statusTextOf(report.statusText), fontSize = 14.sp, fontWeight = FontWeight.Bold)
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

@Composable
private fun AmSatUploadPanel(
    statusName: String,
    upload: AmSatUploadUiState,
    onReportChange: (String) -> Unit,
    onCallsignChange: (String) -> Unit,
    onGridChange: (String) -> Unit,
    onRequestSubmitReport: () -> Unit
) {
    val options = remember(statusName) {
        if (statusName.startsWith("ISS")) UPLOAD_OPTIONS else UPLOAD_OPTIONS.filter { it.apiValue != AMSAT_REPORT_CREW_ACTIVE }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.amsat_upload_report),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.amsat_upload_time_now, formatUtcNowForUpload(System.currentTimeMillis())),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.amsat_upload_status),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { option ->
                val selected = upload.selectedReport == option.apiValue
                FilterChip(
                    selected = selected,
                    onClick = { onReportChange(option.apiValue) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) Color.White else option.color)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(option.labelResId), maxLines = 1)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = option.color.copy(alpha = 0.10f),
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedContainerColor = option.color,
                        selectedLabelColor = Color.White
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = option.color.copy(alpha = 0.60f),
                        selectedBorderColor = option.color
                    )
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = upload.callsign,
                onValueChange = onCallsignChange,
                label = { Text(stringResource(R.string.amsat_upload_callsign)) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = upload.gridSquare,
                onValueChange = onGridChange,
                label = { Text(stringResource(R.string.amsat_upload_grid)) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Button(
            onClick = onRequestSubmitReport,
            enabled = !upload.isSubmitting && upload.callsign.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (upload.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.amsat_upload_submit))
            }
        }
        upload.message?.let { message ->
            if (message == UPLOAD_MESSAGE_SUCCESS) {
                Text(
                    text = stringResource(R.string.amsat_upload_success),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        upload.error?.let { error ->
            Text(
                text = uploadErrorText(error),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun AmSatUploadConfirmDialog(
    statusName: String,
    upload: AmSatUploadUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val option = UPLOAD_OPTIONS.firstOrNull { it.apiValue == upload.selectedReport } ?: UPLOAD_OPTIONS.first()
    val statusLabel = stringResource(option.labelResId)
    val grid = upload.gridSquare.ifBlank { stringResource(R.string.amsat_upload_grid_none) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.amsat_upload_confirm_title)) },
        text = {
            Text(
                text = stringResource(
                    R.string.amsat_upload_confirm_message,
                    statusName,
                    statusLabel,
                    upload.callsign,
                    grid
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !upload.isSubmitting) {
                Text(stringResource(R.string.amsat_upload_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !upload.isSubmitting) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
private fun uploadErrorText(error: String): String {
    return when (error) {
        UPLOAD_ERROR_CALLSIGN_REQUIRED -> stringResource(R.string.amsat_upload_callsign_required)
        UPLOAD_ERROR_GRID_INVALID -> stringResource(R.string.amsat_upload_grid_invalid)
        else -> stringResource(R.string.amsat_upload_failed, error)
    }
}

private val MONTH_ABBR = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun formatFetchedAt(utcMs: Long): String {
    val cal = Calendar.getInstance()
    cal.timeInMillis = utcMs
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val monthIndex = cal.get(Calendar.MONTH)
    val year = cal.get(Calendar.YEAR)
    val hh = cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val mm = cal.get(Calendar.MINUTE).toString().padStart(2, '0')
    val ss = cal.get(Calendar.SECOND).toString().padStart(2, '0')
    return if (Locale.getDefault().language == Locale.CHINESE.language) {
        "${year}年${monthIndex + 1}月${day}日 - $hh:$mm:$ss"
    } else {
        "$day${MONTH_ABBR[monthIndex]} $year - $hh:$mm:$ss"
    }
}

private fun formatUtcNowForUpload(utcMs: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return formatter.format(Date(utcMs))
}
