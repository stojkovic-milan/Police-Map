package com.example.policemap

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.policemap.data.model.Place
import com.example.policemap.data.model.PlaceDb
import com.example.policemap.data.model.User
import com.example.policemap.placeholder.PlaceholderContent
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.*
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

/**
 * A fragment representing a list of Items.
 */
class PlaceListFragment : Fragment() {

    private lateinit var databaseRef: CollectionReference
    private lateinit var placeAdapter: PlaceRecyclerViewAdapter
    private var places: MutableList<Place> = mutableListOf()

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

    override fun onStart() {
        super.onStart()

        // Add a ValueEventListener to fetch and observe changes to the user data
        databaseRef.orderBy("rating").addSnapshotListener { snapshot, error ->
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
}