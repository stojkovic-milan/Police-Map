package com.example.policemap

import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.policemap.data.model.Place
import com.example.policemap.data.model.PlaceDb
import com.example.policemap.data.model.PlaceType
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.google.maps.android.clustering.ClusterManager
import java.util.*
import kotlin.collections.HashMap


class MapsFragment : Fragment(), RatingDialogFragment.RatingDialogCallback,
    MapFilterDrawerFragment.FilterDrawerListener {
    private val db = FirebaseFirestore.getInstance()
    private val placesList = mutableListOf<Place>()
    private lateinit var auth: FirebaseAuth

    //    private val markerRatingMap = HashMap<Marker, Boolean>()
    private val placeRatingMap = HashMap<String, Boolean>()
    private val currentRatingsMap = HashMap<String, Int>()
    private val placesOnMap = HashMap<String, Place>()
    private lateinit var geoFire: GeoFire
    private lateinit var geoQuery: GeoQuery
    private lateinit var clusterManager: ClusterManager<Place>

    private val places: MutableList<Place> = mutableListOf(
        Place(null, LatLng(43.313850, 21.897023), Date(), 4, PlaceType.Radar),
        Place(null, LatLng(43.314952, 21.894705), Date(), 2, PlaceType.Control),
        Place(null, LatLng(43.314952, 21.895705), Date(), -5, PlaceType.Camera),
        Place(null, LatLng(43.315952, 21.897705), Date(), 1, PlaceType.Patrol)
    )
    private var googleMap: GoogleMap? = null
    private var myMarker: Marker? = null
    private var lastLocation: LatLng? = null
    private val database =
        Firebase.database("https://police-map-22d2d-default-rtdb.europe-west1.firebasedatabase.app/")

    private var follow: Boolean = false
    private var addingPlace: Boolean = false
    private var newMarker: Marker? = null

    private lateinit var filterOptions: FilterOptions

    private lateinit var fabFollow: FloatingActionButton
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var fabLeaderboard: FloatingActionButton
    private lateinit var fabFilter: FloatingActionButton
    private lateinit var fabPlacesList: FloatingActionButton

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
        initializeClusterManager(googleMap)
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
        filterOptions = FilterOptions(
            cameraOption = true, radarOption = true, controlOption = true, patrolOption = true,
            showExpired = false,
            showMineOnly = false,
            radius = 2500F
        )
        auth = FirebaseAuth.getInstance()
        val ref = FirebaseDatabase.getInstance().getReference("geofire")
        geoFire = GeoFire(ref)
        val queryCenter = GeoLocation(lastLocation?.latitude ?: 0.0, lastLocation?.longitude ?: 0.0)
        val queryRadius = filterOptions.radius / 1000.0F // in kilometers
        geoQuery = geoFire.queryAtLocation(queryCenter, queryRadius.toDouble())

        initializeGeoQuery()
//        geoQuery.addGeoQueryEventListener(object : GeoQueryEventListener {
//            override fun onKeyEntered(key: String?, location: GeoLocation?) {
//                // Key entered the query area, display the place on the map
//                if (key != null && location != null) {
//                    val placeLatitude = location.latitude
//                    val placeLongitude = location.longitude
//                    // Display the place using its key, latitude, and longitude
//                    //TODO:Filtering here on query instead of locally
//                    val placesCollection = db.collection("places")
//                    var query: Query? = placesCollection.whereEqualTo("id", key)
//
//                    if (!filterOptions.showExpired) {
//                        query = query?.also { query ->
//                            query.whereGreaterThan(
//                                "expirationTime",
//                                Date()
//                            )
//                        }
//                    }
//
//                    if (filterOptions.showMineOnly) {
//                        query = query?.also { query ->
//                            query?.whereEqualTo(
//                                "userId",
//                                auth.currentUser?.uid.toString()
//                            )
//                        }
//                    }
//
//                    val wantedTypesString: MutableList<String> = mutableListOf()
//
//                    if (filterOptions.cameraOption) {
//                        wantedTypesString.add("Camera")
//                    }
//
//                    if (filterOptions.controlOption) {
//                        wantedTypesString.add("Control")
//                    }
//
//                    if (filterOptions.radarOption) {
//                        wantedTypesString.add("Radar")
//                    }
//
//                    if (filterOptions.patrolOption) {
//                        wantedTypesString.add("Patrol")
//                    }
//
//                    if (wantedTypesString.isNotEmpty()) {
//                        query =
//                            query.also { query -> query?.whereIn("placeType", wantedTypesString) }
//                    }
//
//                    query?.get()?.addOnSuccessListener { querySnapshot ->
//                        for (documentSnapshot in querySnapshot.documents) {
//                            val placeDb = documentSnapshot.toObject(PlaceDb::class.java)
//                            if (placeDb != null) {
////                                val expirationTime = placeDb.expirationTime
////                                if (expirationTime != null && expirationTime.before(Date())) {
////                                    // Place has expired, skip further processing
////                                    continue
////                                }
//
//                                // Place is valid, continue processing
//                                keyList.add(key)
//                                val place = Place(
//                                    placeDb.id,
//                                    LatLng(placeDb.lat!!, placeDb.lng!!),
//                                    placeDb.time,
//                                    placeDb.rating,
//                                    placeDb.placeType,
//                                    placeDb.userId,
//                                    placeDb.expirationTime
//                                )
//                                placesOnMap[key] = place
//                                addClusteredMarker(place)
//                            }
//                        }
//                    }
//                        ?.addOnFailureListener { exception ->
//                            // Handle any errors that occurred during the retrieval
//                            // ...
//                        }
//
//                }
//            }
//
//            override fun onKeyExited(key: String?) {
//                // Key exited the query area, remove the place from the map or list view
//                keyList.remove(key)
//                var placeLeft = placesOnMap[key]
//                if (placeLeft != null) {
//                    clusterManager.removeItem(placeLeft)
//                    placesOnMap.remove(key)
//                    clusterManager.cluster()
//                }
//
//            }
//
//            override fun onKeyMoved(key: String?, location: GeoLocation?) {
//                // Key moved within the query area, update the place's position on the map or list view
//            }
//
//            override fun onGeoQueryReady() {
//                // All initial place data has been loaded, do any final processing or UI updates
//            }
//
//            override fun onGeoQueryError(error: DatabaseError?) {
//                // Handle any errors that occurred during the query
//            }
//        })

//        initializeClusterManager()

//        val placesCollection = db.collection("places")
//        placesCollection.get()
//            .addOnSuccessListener { querySnapshot ->
//                // Iterate over the documents in the query snapshot
//                for (document in querySnapshot) {
//                    // Get the data of each document as a Place object
//                    val placeDb = document.toObject(PlaceDb::class.java)
//                    if (placeDb.expirationTime!!.before(Date()))
//                        continue
//                    val place = Place(
//                        placeDb.id,
//                        LatLng(placeDb.lat!!, placeDb.lng!!),
//                        placeDb.time,
//                        placeDb.rating,
//                        placeDb.placeType,
//                        placeDb.userId,
//                        placeDb.expirationTime,
//                    )
//                    // Add the place to the list
//                    placesList.add(place)
//                }
////                addClusteredMarkers(googleMap!!)
////                initializeClusterManager()
//            }
//            .addOnFailureListener { e ->
//                // Handle any errors that occurred during the retrieval
//                // ...
//            }

        fabFollow = view.findViewById(R.id.fabFollow)
        fabAdd = view.findViewById(R.id.fabAdd)
        fabLeaderboard = view.findViewById(R.id.fabLeaderboard)
        fabFilter = view.findViewById(R.id.fabMapFilter)
        fabPlacesList = view.findViewById(R.id.fabPlacesList)
        fabFollow?.setOnClickListener {
            setFollow(!follow)
        }
        fabAdd.setOnClickListener {
            onAddPlace()
        }
        fabLeaderboard.setOnClickListener {
            onShowLeaderboard()
        }
        fabPlacesList.setOnClickListener {
            onShowPlacesList()
        }
        fabFilter.setOnClickListener {
            showFilterDrawer()
        }

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }


    private fun initializeGeoQuery() {
        var keyList: MutableList<String> = mutableListOf()
        geoQuery.addGeoQueryEventListener(object : GeoQueryEventListener {
            override fun onKeyEntered(key: String?, location: GeoLocation?) {
                // Key entered the query area, display the place on the map
                if (key != null && location != null) {
                    val placeLatitude = location.latitude
                    val placeLongitude = location.longitude
                    val placesCollection = db.collection("places")
                    var query: Query? = placesCollection.whereEqualTo("id", key)

                    if (!filterOptions.showExpired) {
                        query = query?.whereGreaterThan("expirationTime", Date())
                    }

                    if (filterOptions.showMineOnly) {
                        query = query?.whereEqualTo("userId", auth.currentUser?.uid.toString())
                    }

                    val wantedTypesString: MutableList<String> = mutableListOf()

                    if (filterOptions.cameraOption) {
                        wantedTypesString.add("Camera")
                    }

                    if (filterOptions.controlOption) {
                        wantedTypesString.add("Control")
                    }

                    if (filterOptions.radarOption) {
                        wantedTypesString.add("Radar")
                    }

                    if (filterOptions.patrolOption) {
                        wantedTypesString.add("Patrol")
                    }

                    query = query?.whereIn("placeType", wantedTypesString)


                    query?.get()?.addOnSuccessListener { querySnapshot ->
                        for (documentSnapshot in querySnapshot.documents) {
                            val placeDb = documentSnapshot.toObject(PlaceDb::class.java)
                            if (placeDb != null) {
                                keyList.add(key)
                                val place = Place(
                                    placeDb.id,
                                    LatLng(placeDb.lat!!, placeDb.lng!!),
                                    placeDb.time,
                                    placeDb.rating,
                                    placeDb.placeType,
                                    placeDb.userId,
                                    placeDb.expirationTime
                                )
                                placesOnMap[key] = place
                                addClusteredMarker(place)
                            }
                        }
                    }
                        ?.addOnFailureListener { exception ->
                            // Handle any errors that occurred during the retrieval
                            // ...
                        }

                }
            }

            override fun onKeyExited(key: String?) {
                // Key exited the query area, remove the place from the map or list view
                keyList.remove(key)
                var placeLeft = placesOnMap[key]
                if (placeLeft != null) {
                    clusterManager.removeItem(placeLeft)
                    placesOnMap.remove(key)
                    clusterManager.cluster()
                }

            }

            override fun onKeyMoved(key: String?, location: GeoLocation?) {
                // Key moved within the query area, update the place's position on the map or list view
            }

            override fun onGeoQueryReady() {
                // All initial place data has been loaded, do any final processing or UI updates
            }

            override fun onGeoQueryError(error: DatabaseError?) {
                // Handle any errors that occurred during the query
            }
        })
    }

    private fun onShowLeaderboard() {
        val userListFragment =
            UserListFragment.newInstance()
        parentFragmentManager.beginTransaction()
            .add(R.id.fragment_container, userListFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun onShowPlacesList() {
        val placesListFragment =
            PlaceListFragment.newInstance()
        parentFragmentManager.beginTransaction()
            .add(R.id.fragment_container, placesListFragment)
            .addToBackStack(null)
            .commit()
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

            Snackbar.make(
                view!!,
                "Drag marker to accurate location of report!",
                Snackbar.LENGTH_LONG
            )
                .setAnchorView(R.id.fab)
                .setAction("Action", null).show()

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
        val newFragment = AddPlaceDialogFragment(newMarker!!)
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
    private fun addClusteredMarker(place: Place) {
        currentRatingsMap[place.id!!] = place.rating!!
        //Attach listener to keep rating updated
        val placeRef = db.collection("places").document(place.id!!)
        val listener = placeRef.addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                // Handle any errors that occurred
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val rating = snapshot.getLong("rating")?.toInt()
                // Handle the updated rating value here
                if (rating != null)
                    currentRatingsMap[place.id] = rating
            }
        }
        // Add the place to the ClusterManager.
        checkIfPlaceRatedByUser(
            place.id!!,
            auth.currentUser?.uid.toString()
        ) { isRated ->
            placeRatingMap[place.id] = isRated
        }
        clusterManager.addItem(place)
        clusterManager.cluster()
    }

    private fun initializeClusterManager(googleMap: GoogleMap) {
// Create the ClusterManager class and set the custom renderer.
        clusterManager = ClusterManager<Place>(requireContext(), googleMap)
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
                placeRatingMap,
                currentRatingsMap
            )
        )
        clusterManager.markerCollection.setOnInfoWindowClickListener {
            val place = it?.tag as? Place ?: return@setOnInfoWindowClickListener
            //Check if user has already rated this report
            val isRated: Boolean = placeRatingMap[place.id] ?: false

            if (isRated) {
//                markerRatingMap[it] = true
                // The place has been rated by the user
                // Do something
                //TODO: Add snackbar that says place is already rated by you
            } else {
                // The place has not been rated by the user
                // Do something else
//                markerRatingMap[it] = false
                val distance = calculateDistanceBetweenLatLng(place.latLng!!, lastLocation!!)
                //Can rate places in radius of 3KM
                if (distance < 3000) {
                    it.hideInfoWindow()
                    showRateDialog(place)
                } else
                    Toast.makeText(
                        context,
                        "You are too far to rate this report!",
                        Toast.LENGTH_LONG
                    ).show()

            }

        }
        clusterManager.cluster()

        // Set ClusterManager as the OnCameraIdleListener so that it
        // can re-cluster when zooming in and out.
        googleMap.setOnCameraIdleListener {
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

    fun calculateDistanceBetweenLocations(point1: Location, point2: Location): Float {
        return point1.distanceTo(point2)
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
        Toast.makeText(
            context,
            "Rating received %d back for place %s".format(rating, placeId),
            Toast.LENGTH_LONG
        ).show()
        updatePlaceRatingDb(placeId, rating)
    }

    private fun updatePlaceRatingDb(placeId: String, increment: Int) {

        val placeRef = db.collection("places").document(placeId)
        val userId = auth.currentUser?.uid.toString()
        var reportingUserId: String? = null
//TODO: Check this
//Using transaction to ensure atomicity when updating rating
        db.runTransaction { transaction ->
            val placeDoc = transaction.get(placeRef)
            val currentRating = placeDoc.getLong("rating") ?: 0
            val newRating = currentRating + increment
            reportingUserId = placeDoc.getString("userId")
            transaction.update(placeRef, "rating", newRating)

            //Extend/Decrease expiration time for 5 minutes per each rating
            val currentExpTime = placeDoc.getDate("expirationTime")
            val expirationTime: Calendar = Calendar.getInstance()
            expirationTime.time = currentExpTime
            expirationTime.add(Calendar.MINUTE, 5 * increment)
            transaction.update(placeRef, "expirationTime", expirationTime.time)
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

            //Adding to map so the view for user who rated can be updated immediately
            placeRatingMap[placeId] = true

            if (userId != null && reportingUserId != null) {
                val currentUserRef =
                    database.reference.child("users").child(userId)
                val reportingUserRef =
                    database.reference.child("users").child(reportingUserId!!)

                currentUserRef.get().addOnSuccessListener { userDataSnapshot ->
                    val currentPoints =
                        userDataSnapshot.child("points").getValue(Int::class.java) ?: 0
                    val increasedPoints = currentPoints + 10
                    currentUserRef.child("points").setValue(increasedPoints)
                        .addOnSuccessListener {
                            Toast.makeText(
                                requireContext(),
                                "You just gained 10 pints for rating!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                reportingUserRef.get().addOnSuccessListener { userDataSnapshot ->
                    val currentPoints =
                        userDataSnapshot.child("points").getValue(Int::class.java) ?: 0
                    val increasedPoints = currentPoints + 20
                    reportingUserRef.child("points").setValue(increasedPoints)
                }
            }
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

    fun convertGeoLocationToLocation(geoLocation: GeoLocation): Location {
        val location = Location("GeoLocation")
        location.latitude = geoLocation.latitude
        location.longitude = geoLocation.longitude
        return location
    }

    fun updateLocation(currentLocation: Location?) {
        val latLng = LatLng(currentLocation!!.latitude, currentLocation.longitude)
        lastLocation = latLng
        //Update GeoQuery center only if there is at least 5 meters distance between new location and old query center
        //TODO: Change minimum distance to 10m? So not every request changes query location
        if (currentLocation.distanceTo(convertGeoLocationToLocation(geoQuery.center)) >= 5) {
            val newQueryCenter = GeoLocation(currentLocation.latitude, currentLocation.longitude)
            geoQuery.center = newQueryCenter
        }
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

    private fun showFilterDrawer() {
        val filterDrawerFragment =
            MapFilterDrawerFragment(
                camera = filterOptions.cameraOption,
                radar = filterOptions.radarOption,
                control = filterOptions.controlOption,
                patrol = filterOptions.patrolOption,
                radius = filterOptions.radius,
                expired = filterOptions.showExpired,
                mineOnly = filterOptions.showMineOnly
            )
        filterDrawerFragment.setRatingDialogCallback(this)
        filterDrawerFragment.show(parentFragmentManager, "FilterDrawerFragment")
    }

    override fun onFilterApplied(
        cameraOption: Boolean,
        radarOption: Boolean,
        controlOption: Boolean,
        patrolOption: Boolean,
        radius: Float,
        showExpired: Boolean,
        showMineOnly: Boolean
    ) {
        filterOptions = FilterOptions(
            cameraOption,
            radarOption,
            controlOption,
            patrolOption,
            showExpired,
            showMineOnly,
            radius
        )
        geoQuery.radius = (radius / 1000F).toDouble()
        geoQuery.removeAllListeners()
        clusterManager.removeItems(placesOnMap.values)
        clusterManager.cluster()
        //Clearing current maps
        //Test?
        placesOnMap.clear()
        placeRatingMap.clear()
        currentRatingsMap.clear()
        //
        initializeGeoQuery()
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