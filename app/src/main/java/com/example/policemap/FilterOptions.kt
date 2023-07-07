package com.example.policemap

data class FilterOptions(
    val cameraOption: Boolean,
    val radarOption: Boolean,
    val controlOption: Boolean,
    val patrolOption: Boolean,
    val showExpired: Boolean,
    val showMineOnly: Boolean,
    val radius: Float
)