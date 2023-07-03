package com.example.policemap

import android.content.Context
import androidx.core.content.ContextCompat
import com.example.policemap.data.model.Place
import com.example.policemap.data.model.Type
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer

/**
 * A custom cluster renderer for Place objects.
 */
class PlaceRenderer(
    private val context: Context,
    map: GoogleMap,
    clusterManager: ClusterManager<Place>
) : DefaultClusterRenderer<Place>(context, map, clusterManager) {

    private val stopIcon: BitmapDescriptor by lazy {
//        val color = ContextCompat.getColor(requireContext(), R.color.purple_500)
        BitmapHelper.vectorToBitmap(context, R.drawable.police_stop_48)
    }
    private val radarIcon: BitmapDescriptor by lazy {
        BitmapHelper.vectorToBitmap(context, R.drawable.police_radar_32)
    }
    private val cameraIcon: BitmapDescriptor by lazy {
        BitmapHelper.vectorToBitmap(context, R.drawable.police_camera_32)
    }
    private val patrolIcon: BitmapDescriptor by lazy {
        BitmapHelper.vectorToBitmap(context, R.drawable.police_patrol_32)
    }

    /**
     * Method called before the cluster item (the marker) is rendered.
     * This is where marker options should be set.
     */
    override fun onBeforeClusterItemRendered(
        item: Place,
        markerOptions: MarkerOptions
    ) {
        markerOptions.title(item.name)
            .position(item.latLng)
            .icon(
                when (item.type) {
                    Type.Radar -> radarIcon
                    Type.Control -> stopIcon
                    Type.Camera -> cameraIcon
                    else -> patrolIcon
                }
            )
    }

    /**
     * Method called right after the cluster item (the marker) is rendered.
     * This is where properties for the Marker object should be set.
     */
    override fun onClusterItemRendered(clusterItem: Place, marker: Marker) {
        marker.tag = clusterItem
    }
}