package com.example.policemap.data.model

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import java.util.*

data class Place(
    val name: String,
    val latLng: LatLng,
//    val address: LatLng,
    val time: Date,
    val rating: Float,
    val type: Type
) : ClusterItem {
    override fun getPosition(): LatLng =
        latLng

    override fun getTitle(): String =
        name

    override fun getSnippet(): String =
        time.toString()
}

enum class Type {
    Control, Patrol, Radar, Camera
}