package com.rtbishop.look4sat.core.domain.model

/** One public AMSAT satellite status report submission. */
data class AmSatReportSubmission(
    val name: String,
    val report: String,
    val callsign: String,
    val gridSquare: String,
    val reportedAtUtcMillis: Long
)

/** Result of submitting an AMSAT satellite status report. */
data class AmSatReportSubmitResult(
    val success: Boolean,
    val message: String = "",
    val reportId: String? = null
)
