package com.example.policemap.data.model

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import java.util.*

data class Place(
    val id: String? = null,
//    val name: String? = null,
    val latLng: LatLng? = null,
    val time: Date? = null,
    val rating: Int? = null,
    val placeType: PlaceType? = null,
    val userId: String? = null,
    val expirationTime: Date? = null,
//    val anon: Boolean? = true
) : ClusterItem {
    override fun getPosition(): LatLng =
        latLng!!

    override fun getTitle(): String =
        placeType.toString()

    override fun getSnippet(): String =
        time.toString()
}

enum class PlaceType {
    Control, Patrol, Radar, Camera
}