package com.counhopig.ccalendar.ui.model

import androidx.compose.ui.graphics.Color

data class Calendar(
    val id: Long,
    val name: String,
    val color: Color,
    val accountName: String = "",
    val accountType: String = ""
)
