package com.example.policemap

import android.location.Geocoder
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.policemap.data.model.Place
import com.example.policemap.data.model.PlaceType

import com.example.policemap.databinding.FragmentPlaceListBinding
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.material.chip.Chip
import com.google.firebase.database.*
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import java.util.*

/**
 * [RecyclerView.Adapter] that can display a [Place].
 */
class PlaceRecyclerViewAdapter(
    private val values: List<Place>
) : RecyclerView.Adapter<PlaceRecyclerViewAdapter.ViewHolder>() {
    private lateinit var databaseRef: DatabaseReference

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        databaseRef = FirebaseDatabase.getInstance().getReference("users")

        return ViewHolder(
            FragmentPlaceListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.typeView.text = item.placeType.toString()
        val pattern = "HH:mm:ss  dd.MM.yyyy"
        val simpleDateFormat = SimpleDateFormat(pattern)
        holder.timeView.text = "Reported: %s".format(simpleDateFormat.format(item.time))
        var expired = false
        if (item.expirationTime!! < Date()) {
            expired = true
            holder.expiredChip.visibility = View.VISIBLE
        } else {
            holder.expiredChip.visibility = View.GONE
        }
        holder.expirationView.text =
            (if (expired) "Expired: %s" else "Expires: %s").format(simpleDateFormat.format((item.expirationTime)))
        val userRef = databaseRef.child(item.userId!!)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.hasChildren()) {
                    val username = dataSnapshot.child("username").getValue(String::class.java)

                    if (username != null) {
                        holder.userView.text = "Reported by: %s".format(username)
                    } else {
                        holder.userView.text = "Reported by: %s".format(item.userId)
                    }
                } else {
                    holder.userView.text = item.userId
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                holder.userView.text = item.userId
            }
        })


        val geocoder = Geocoder(holder.addressView.context, Locale.getDefault())

        val addressList = geocoder.getFromLocation(
            item.latLng?.latitude!!,
            item.latLng.longitude!!,
            1
        )
        if (addressList?.isNotEmpty() == true)
            holder.addressView.text = addressList[0].getAddressLine(0)
        else
            holder.addressView.text = "Unknown Address"

        holder.ratingView.text = "Rating: %d".format(item.rating)

        val drawable = ContextCompat.getDrawable(
            holder.iconView.context,
            when (item.placeType) {
                PlaceType.Radar -> R.drawable.police_radar_32
                PlaceType.Control -> R.drawable.police_stop_32
                PlaceType.Camera -> R.drawable.police_camera_32
                else -> R.drawable.police_patrol_32
            }
        )


        holder.iconView.setImageDrawable(drawable)
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(binding: FragmentPlaceListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val typeView: TextView = binding.placeType
        val addressView: TextView = binding.placeAddress
        val timeView: TextView = binding.placeTime
        val expirationView: TextView = binding.placeExpiration
        val userView: TextView = binding.placeUser
        val ratingView: TextView = binding.placeRating
        val iconView: ImageView = binding.reportIcon
        val expiredChip: Chip = binding.chipExpired


        override fun toString(): String {
            return super.toString() + " '" + typeView.text + "'"
        }
    }

}