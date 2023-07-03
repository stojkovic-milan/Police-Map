package com.example.policemap

import android.location.Location
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.example.policemap.data.model.Place
import com.example.policemap.data.model.Type

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import java.util.*

class MapsFragment : Fragment() {

    private val places: List<Place> = listOf(
        Place("Radar", LatLng(43.313850, 21.897023), Date(), 4.8F, Type.Radar),
        Place("Kontrola", LatLng(43.314952, 21.894705), Date(), 2.1F, Type.Control),
        Place("Kamera", LatLng(43.314952, 21.895705), Date(), 4.5F, Type.Camera),
        Place("Patrola", LatLng(43.315952, 21.897705), Date(), 4.1F, Type.Patrol)
    )
    private var googleMap: GoogleMap? = null
    private var myMarker: Marker? = null

    private val callback = OnMapReadyCallback { googleMap ->
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        this.googleMap = googleMap
        addClusteredMarkers(googleMap)

        val mainActivity = activity as MainActivity
        val currentLocation = mainActivity.getCurrentLocation()

        updateLocation(currentLocation)
    }
    private val waypointIcon: BitmapDescriptor by lazy {
        BitmapHelper.vectorToBitmap(requireContext(), R.drawable.pointer_32)
    }
    private val gpsIcon: BitmapDescriptor by lazy {
        BitmapHelper.vectorToBitmap(requireContext(), R.drawable.gps_16)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    /**
     * Adds markers to the map with clustering support.
     */
    private fun addClusteredMarkers(googleMap: GoogleMap) {
        // Create the ClusterManager class and set the custom renderer.
        val clusterManager = ClusterManager<Place>(requireContext(), googleMap)
        clusterManager.renderer =
            PlaceRenderer(
                requireContext(),
                googleMap,
                clusterManager
            )

        // Set custom info window adapter
        clusterManager.markerCollection.setInfoWindowAdapter(MarkerInfoWindowAdapter(requireContext()))

        // Add the places to the ClusterManager.
        clusterManager.addItems(places)
        clusterManager.cluster()

        // Set ClusterManager as the OnCameraIdleListener so that it
        // can re-cluster when zooming in and out.
        googleMap.setOnCameraIdleListener {
            // When the camera stops moving, change the alpha value back to opaque.
//            clusterManager.markerCollection.markers.forEach { it.alpha = 1.0f }
//            clusterManager.clusterMarkerCollection.markers.forEach { it.alpha = 1.0f }

            // Call clusterManager.onCameraIdle() when the camera stops moving so that reclustering
            // can be performed when the camera stops moving.
            clusterManager.onCameraIdle()
        }
    }

    fun updateLocation(currentLocation: Location?) {
        val latLng = LatLng(currentLocation!!.latitude, currentLocation.longitude)
        val markerOptions = MarkerOptions().position(latLng).title("Vi ste ovde!").icon(gpsIcon)
//        googleMap?.animateCamera(CameraUpdateFactory.newLatLng(latLng))
        if (myMarker == null)
            myMarker = googleMap?.addMarker(markerOptions)
        else
            myMarker!!.position = latLng
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }
}