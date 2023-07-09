package com.example.policemap

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.policemap.data.FilterOptions
import com.example.policemap.data.FilterSortOptions
import com.example.policemap.data.SortOptions
import com.example.policemap.data.model.Place
import com.example.policemap.data.model.PlaceDb
import com.example.policemap.data.model.User
import com.example.policemap.placeholder.PlaceholderContent
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

/**
 * A fragment representing a list of Items.
 */
class PlaceListFragment : Fragment(), MapFilterDrawerFragment.FilterDrawerListener {

    private lateinit var databaseRef: CollectionReference
    private lateinit var placeAdapter: PlaceRecyclerViewAdapter
    private var places: MutableList<Place> = mutableListOf()
    private lateinit var btnFilter: ImageButton
    private lateinit var filterSortOptions: FilterSortOptions
    private var snapshotListener: ListenerRegistration? = null
    private lateinit var auth: FirebaseAuth


    private var columnCount = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        databaseRef = FirebaseFirestore.getInstance().collection("places")
//        FirebaseDatabase.getInstance().getReference("places")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_place_list_list, container, false)

        // Set up the RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.list)
        recyclerView.layoutManager = LinearLayoutManager(context)
        placeAdapter = PlaceRecyclerViewAdapter(places)
        recyclerView.adapter = placeAdapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        filterSortOptions = FilterSortOptions(
            SortOptions.RatingD, FilterOptions(
                cameraOption = true, radarOption = true, controlOption = true, patrolOption = true,
                showExpired = true,
                showMineOnly = false,
                radius = 5000F
            )
        )
        auth = FirebaseAuth.getInstance()
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.beginTransaction().remove(this).commit()
        }
        btnFilter = view.findViewById(R.id.btn_filter)
        btnFilter.setOnClickListener {
            showFilterDrawer()
        }
        val spinnerSort = view.findViewById<Spinner>(R.id.spinner_sort)
        spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
//                val selectedItem = parent?.getItemAtPosition(position).toString()
                when (position) {
                    0 -> filterSortOptions.sortBy = SortOptions.RatingD
                    1 -> filterSortOptions.sortBy = SortOptions.RatingA
                    2 -> filterSortOptions.sortBy = SortOptions.TimeD
                    else -> filterSortOptions.sortBy = SortOptions.TimeA
                }
                onFilterSortChanged()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
//                TODO("Not yet implemented")
            }
        }
    }

    private fun onFilterSortChanged() {
        snapshotListener?.remove()
        var query: Query? = databaseRef

        if (!filterSortOptions.filterOptions.showExpired) {
            val currentTimestamp = Timestamp.now()
            query = query?.whereGreaterThan("expirationTime", currentTimestamp)
        }

        if (filterSortOptions.filterOptions.showMineOnly) {
            query = query?.whereEqualTo("userId", auth.currentUser?.uid.toString())
        }

        val wantedTypesString: MutableList<String> = mutableListOf()

        if (filterSortOptions.filterOptions.cameraOption) {
            wantedTypesString.add("Camera")
        }

        if (filterSortOptions.filterOptions.controlOption) {
            wantedTypesString.add("Control")
        }

        if (filterSortOptions.filterOptions.radarOption) {
            wantedTypesString.add("Radar")
        }

        if (filterSortOptions.filterOptions.patrolOption) {
            wantedTypesString.add("Patrol")
        }

        query = query?.whereIn("placeType", wantedTypesString)

        snapshotListener =
            query
//        ?.orderBy(
//                if (filterSortOptions.sortBy == SortOptions.RatingD
//                    || filterSortOptions.sortBy == SortOptions.RatingA
//                ) "rating" else "time"
//            )
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
//                        places.clear()
//                        placeAdapter.notifyDataSetChanged()
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        places.clear()
                        var results = snapshot.documents
                        when (filterSortOptions.sortBy) {
                            SortOptions.TimeD -> results.sortByDescending { r -> r.getDate("time") }
                            SortOptions.TimeA -> results.sortBy { r -> r.getDate("time") }
                            SortOptions.RatingD -> results.sortByDescending { r -> r.getDouble("rating") }
                            else -> results.sortBy { r -> r.getDouble("rating") }
                        }

                        for (documentSnapshot in results) {
                            val placeDb = documentSnapshot.toObject(PlaceDb::class.java)
                            if (placeDb != null) {
                                val place = Place(
                                    placeDb.id,
                                    LatLng(placeDb.lat!!, placeDb.lng!!),
                                    placeDb.time,
                                    placeDb.rating,
                                    placeDb.placeType,
                                    placeDb.userId,
                                    placeDb.expirationTime
                                )
                                place.let {
                                    places.add(it)
                                }
                            }
                        }
                        placeAdapter.notifyDataSetChanged()
                    }
                }
    }

    private fun showFilterDrawer() {
        val filterDrawerFragment =
            MapFilterDrawerFragment(
                camera = filterSortOptions.filterOptions.cameraOption,
                radar = filterSortOptions.filterOptions.radarOption,
                control = filterSortOptions.filterOptions.controlOption,
                patrol = filterSortOptions.filterOptions.patrolOption,
                radius = filterSortOptions.filterOptions.radius,
                expired = filterSortOptions.filterOptions.showExpired,
                mineOnly = filterSortOptions.filterOptions.showMineOnly
            )
        filterDrawerFragment.setRatingDialogCallback(this)
        filterDrawerFragment.show(parentFragmentManager, "FilterDrawerFragment")
    }

    override fun onStart() {
        super.onStart()
        // Add a ValueEventListener to fetch and observe changes to the user data
        snapshotListener = databaseRef.orderBy("rating").addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Handle database error
                return@addSnapshotListener
            }

            if (snapshot != null) {
                places.clear()
                for (documentSnapshot in snapshot.documents.reversed()) {
                    val placeDb = documentSnapshot.toObject(PlaceDb::class.java)
                    if (placeDb != null) {
                        val place = Place(
                            placeDb.id,
                            LatLng(placeDb.lat!!, placeDb.lng!!),
                            placeDb.time,
                            placeDb.rating,
                            placeDb.placeType,
                            placeDb.userId,
                            placeDb.expirationTime
                        )
                        place.let {
                            places.add(it)
                        }
                    }
                }
                placeAdapter.notifyDataSetChanged()
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = PlaceListFragment()
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

        filterSortOptions.filterOptions = FilterOptions(
            cameraOption,
            radarOption,
            controlOption,
            patrolOption,
            showExpired,
            showMineOnly,
            radius
        )
        onFilterSortChanged()
    }
}