package com.example.policemap.data.model

import com.google.android.gms.maps.model.LatLng
import java.util.*

data class Place(
    val name: String,
    val latLng: LatLng,
//    val address: LatLng,
    val time: Date,
    val rating: Float,
    val type: Type
)

enum class Type {
    Control, Patrol, Radar, Camera
}