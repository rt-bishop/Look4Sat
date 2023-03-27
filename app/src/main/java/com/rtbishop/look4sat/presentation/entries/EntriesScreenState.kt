package com.rtbishop.look4sat.presentation.entries

import com.rtbishop.look4sat.model.SatItem

data class EntriesScreenState(
    val isLoading: Boolean,
    val itemsList: List<SatItem>,
    val currentType: String,
    val typesList: List<String>,
)
