package com.example.policemap

import android.location.Location
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction
import com.example.policemap.data.model.Place
import com.example.policemap.data.model.PlaceDb
import com.example.policemap.data.model.PlaceType

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.clustering.ClusterManager
import java.util.*

class MapsFragment : Fragment(), RatingDialogFragment.RatingDialogCallback {
    private val db = FirebaseFirestore.getInstance()
    private val placesList = mutableListOf<Place>()
    private lateinit var auth: FirebaseAuth
    private val markerRatingMap = HashMap<Marker, Boolean>()
    private val placeRatingMap = HashMap<Place, Boolean>()

    private val places: MutableList<Place> = mutableListOf(
        Place(null, LatLng(43.313850, 21.897023), Date(), 4, PlaceType.Radar),
        Place(null, LatLng(43.314952, 21.894705), Date(), 2, PlaceType.Control),
        Place(null, LatLng(43.314952, 21.895705), Date(), -5, PlaceType.Camera),
        Place(null, LatLng(43.315952, 21.897705), Date(), 1, PlaceType.Patrol)
    )
    private var googleMap: GoogleMap? = null
    private var myMarker: Marker? = null
    private var lastLocation: LatLng? = null

    private var follow: Boolean = false
    private var addingPlace: Boolean = false
    private var newMarker: Marker? = null

    private var fabFollow: FloatingActionButton? = null
    private lateinit var fabAdd: FloatingActionButton
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
//        addClusteredMarkers(googleMap)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(43.313850, 21.897023), 15f))
        val mainActivity = activity as MainActivity
        val currentLocation = mainActivity.getCurrentLocation()
        googleMap.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE)
                setFollow(false)
        }
//        googleMap.setOnMarkerDragListener(object : OnMarkerDragListener {
//            override fun onMarkerDragStart(marker: Marker?) {
//                TODO("Not yet implemented")
//            }
//
//            override fun onMarkerDrag(marker: Marker?) {
//                TODO("Not yet implemented")
//            }
//
//            override fun onMarkerDragEnd(marker: Marker?) {
//                TODO("Not yet implemented")
////                markerPos = marker.position
//            }
//        })

        updateLocation(currentLocation)

    }
    private val waypointIcon: BitmapDescriptor by lazy {
        BitmapHelper.vectorToBitmap(requireContext(), R.drawable.pointer_32)
    }
    private val gpsIcon: BitmapDescriptor by lazy {
        BitmapHelper.vectorToBitmap(requireContext(), R.drawable.gps_26)
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
        auth = FirebaseAuth.getInstance()

        val placesCollection = db.collection("places")
        placesCollection.get()
            .addOnSuccessListener { querySnapshot ->
                // Iterate over the documents in the query snapshot
                for (document in querySnapshot) {
                    // Get the data of each document as a Place object
                    val placeDb = document.toObject(PlaceDb::class.java)
                    val place = Place(
                        placeDb.id,
                        LatLng(placeDb.lat!!, placeDb.lng!!),
                        placeDb.time,
                        placeDb.rating,
                        placeDb.placeType,
                        placeDb.userId,
                        placeDb.expirationTime,
                    )
                    // Add the place to the list
                    placesList.add(place)
                }
                addClusteredMarkers(googleMap!!)
            }
            .addOnFailureListener { e ->
                // Handle any errors that occurred during the retrieval
                // ...
            }

        fabFollow = view.findViewById(R.id.fabFollow)
        fabAdd = view.findViewById(R.id.fabAdd)
        fabFollow?.setOnClickListener {
            setFollow(!follow)
        }
        fabAdd.setOnClickListener {
            onAddPlace()
        }
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    private fun setFollow(newValue: Boolean) {
        follow = newValue
        if (newValue) {
            val location = Location("CurrentLocation")
            location.latitude = myMarker?.position?.latitude!!
            location.longitude = myMarker?.position?.longitude!!
            updateLocation(location)
        }
        fabFollow?.imageTintList = null
        fabFollow?.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                if (newValue) R.drawable.gps_32 else R.drawable.gps_black_64
            )
        )
    }

    private val addIcon: BitmapDescriptor by lazy {
        BitmapHelper.vectorToBitmap(requireContext(), R.drawable.add_location_32)
    }

    private fun onAddPlace() {
        if (!addingPlace) {
            //Add new temporary draggable marker
            addingPlace = true
            val latLng = lastLocation
            val markerOptions =
                MarkerOptions().position(
                    (if (latLng != null) latLng
                    else googleMap?.cameraPosition?.target)!!
                )
                    .title("Nova lokacija").icon(addIcon)
                    .zIndex(1.0f)
                    .draggable(true)
//                    .anchor(0.5f, 0.5f) // Set the anchor point to the center of the marker icon
//                    .infoWindowAnchor(
//                        0.5f,
//                        0.5f
//                    ) // Set the info window anchor point to the center of the marker icon
            Toast.makeText(
                requireContext(),
                "Drag marker to accurate location of report!",
                Toast.LENGTH_LONG
            ).show()
            newMarker = googleMap?.addMarker(markerOptions)
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18F))
        } else {
            //Confirm marker location and enter next data
//            fabAdd.hide()
            addingPlace = false
            newMarker?.setIcon(stopIcon)

            //Centering on location in rest of the screen, under dialog
            googleMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(
                        newMarker!!.position.latitude + 0.0008F,
                        newMarker!!.position.longitude
                    ), 18F
                )
            )

            showDialog()
//            places.add(Place("Novo Mesto", newMarker!!.position, Date(), 1.0f, Type.Control))
//            newMarker!!.remove()
//            addClusteredMarkers(googleMap!!)

        }
        fabAdd?.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                if (addingPlace) R.drawable.checkmark_512 else R.drawable.add_location
            )
        )
    }

    private fun showDialog() {
        val fragmentManager = parentFragmentManager
        val newFragment = AddPlaceDialogFragment()
        //Passing location to addPlaceDialog
        val arguments = Bundle()
        arguments.putDouble("lat", newMarker?.position?.latitude ?: 0.0)
        arguments.putDouble("lng", newMarker?.position?.longitude ?: 0.0)
        newFragment.arguments = arguments

        // The device is smaller, so show the fragment fullscreen
        val transaction = fragmentManager.beginTransaction()
        // For a little polish, specify a transition animation
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        // To make it fullscreen, use the 'content' root view as the container
        // for the fragment, which is always the root view for the activity
        transaction
            .add(R.id.fragment_container, newFragment)
            .addToBackStack(null)
            .commit()
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
        clusterManager.markerCollection.setInfoWindowAdapter(
            MarkerInfoWindowAdapter(
                requireContext(),
//                markerRatingMap
                placeRatingMap
            )
        )
        clusterManager.markerCollection.setOnInfoWindowClickListener {
            val place = it?.tag as? Place ?: return@setOnInfoWindowClickListener
            //Check if user has already rated this report
            val isRated: Boolean = placeRatingMap[place] ?: false

            if (isRated) {
                markerRatingMap[it] = true
                // The place has been rated by the user
                // Do something
            } else {
                // The place has not been rated by the user
                // Do something else
                markerRatingMap[it] = false
                val distance = calculateDistanceBetweenLatLng(place.latLng!!, lastLocation!!)
                //Can rate places in radius of 3KM
                if (distance < 3000)
                    showRateDialog(place)
                else
                    Toast.makeText(
                        context,
                        "You are too far to rate this report!",
                        Toast.LENGTH_LONG
                    ).show()

            }

        }

        // Add the places to the ClusterManager.
        //TODO: Optimise this part, dont check for every place, check instead only for needed places
        // or it will be optimised later when using GeoFire
        placesList.forEach {
            checkIfPlaceRatedByUser(
                it.id!!,
                auth.currentUser?.uid.toString()
            ) { isRated ->
//                markerRatingMap[it] = isRated
                placeRatingMap[it] = isRated
            }
        }
        clusterManager.addItems(placesList)
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


    fun calculateDistanceBetweenLatLng(point1: LatLng, point2: LatLng): Float {
        val location1 = Location("point1")
        location1.latitude = point1.latitude
        location1.longitude = point1.longitude

        val location2 = Location("point2")
        location2.latitude = point2.latitude
        location2.longitude = point2.longitude

        return location1.distanceTo(location2)
    }

    private fun checkIfPlaceRatedByUser(
        placeId: String,
        userId: String,
        callback: (Boolean) -> Unit
    ) {
        val usersWhoRatedRef = db.collection("places").document(placeId).collection("usersWhoRated")
        usersWhoRatedRef.document(userId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val isRated = documentSnapshot.exists()
                callback(isRated)
            }
            .addOnFailureListener { exception ->
                // Handle the error here
                callback(false) // Assume it is not rated in case of an error
            }
    }

    private fun findPlaceByLocation(desiredLocation: LatLng): Place? {
        for (place in placesList) {
            if (place.latLng == desiredLocation) {
                return place
            }
        }
        return null
    }

    private fun findPlaceById(id: String): Place? {
        for (place in placesList) {
            if (place.id == id) {
                return place
            }
        }
        return null
    }

    private fun showRateDialog(place: Place) {
        val fragmentManager = parentFragmentManager
        val newFragment = RatingDialogFragment()
        newFragment.setRatingDialogCallback(this)

        //Passing location to addPlaceDialog
        val arguments = Bundle()
        arguments.putInt("rating", place.rating!!)
        arguments.putString("id", place.id)
        newFragment.arguments = arguments

        // The device is smaller, so show the fragment fullscreen
        val transaction = fragmentManager.beginTransaction()
        // For a little polish, specify a transition animation
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        // To make it fullscreen, use the 'content' root view as the container
        // for the fragment, which is always the root view for the activity
        transaction
            .add(android.R.id.content, newFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onRatingSubmitted(placeId: String, rating: Int) {
        // Handle the submitted rating here
        // You can update the map or perform any other action based on the rating
        Toast.makeText(
            context,
            "Rating received %d back for place %s".format(rating, placeId),
            Toast.LENGTH_LONG
        ).show()
        updatePlaceRatingDb(placeId, rating)
    }

    private fun updatePlaceRatingDb(placeId: String, increment: Int) {

        val placeRef = db.collection("places").document(placeId)
//TODO: Check this
//Using transaction to ensure atomicity when updating rating
        db.runTransaction { transaction ->
            val userId = auth.currentUser?.uid.toString()
            val placeDoc = transaction.get(placeRef)
            val currentRating = placeDoc.getLong("rating") ?: 0
            val newRating = currentRating + increment
            transaction.update(placeRef, "rating", newRating)
            //Saving users that rated place
            val usersWhoRatedRef =
                placeRef.collection("usersWhoRated") // create a subcollection called "usersWhoRated"
            transaction.set(usersWhoRatedRef.document(userId), mapOf("rated" to true))
        }.addOnSuccessListener {
            Toast.makeText(
                context,
                "Rating updated successfully!",
                Toast.LENGTH_LONG
            ).show()
            var ratedPlace = findPlaceById(placeId)
            //TODO: Update place rating on map
        }.addOnFailureListener { e ->
            // Error occurred during the rating update
            // Handle the error here
            Toast.makeText(
                context,
                "Faiure updating rating!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun updateLocation(currentLocation: Location?) {
        val latLng = LatLng(currentLocation!!.latitude, currentLocation.longitude)
        lastLocation = latLng
        val markerOptions = MarkerOptions().position(latLng).title("Vi ste ovde!").icon(gpsIcon)
            .anchor(0.5f, 0.5f) // Set the anchor point to the center of the marker icon
            .infoWindowAnchor(
                0.5f,
                0.5f
            ) // Set the info window anchor point to the center of the marker icon

//        googleMap?.animateCamera(CameraUpdateFactory.newLatLng(latLng))
        if (myMarker == null)
            myMarker = googleMap?.addMarker(markerOptions)
        else
            myMarker!!.position = latLng
        if (follow) {
            val zoomLevel = googleMap!!.cameraPosition.zoom // Get the current zoom level
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel)
            googleMap?.animateCamera(cameraUpdate)
        }
//        googleMap?.animateCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    private val stopIcon: BitmapDescriptor by lazy {
//        val color = ContextCompat.getColor(requireContext(), R.color.purple_500)
        BitmapHelper.vectorToBitmap(requireContext(), R.drawable.police_stop_48)
    }
    private val radarIcon: BitmapDescriptor by lazy {
        BitmapHelper.vectorToBitmap(requireContext(), R.drawable.police_radar_32)
    }
    private val cameraIcon: BitmapDescriptor by lazy {
        BitmapHelper.vectorToBitmap(requireContext(), R.drawable.police_camera_32)
    }
    private val patrolIcon: BitmapDescriptor by lazy {
        BitmapHelper.vectorToBitmap(requireContext(), R.drawable.police_patrol_32)
    }
}