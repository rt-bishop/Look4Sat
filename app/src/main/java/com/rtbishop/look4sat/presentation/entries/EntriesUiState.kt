package com.rtbishop.look4sat.presentation.entries

import com.rtbishop.look4sat.domain.model.SatItem

data class EntriesUiState(
    val isLoading: Boolean,
    val itemsList: List<SatItem>,
    val currentType: String,
    val typesList: List<String>,
)
