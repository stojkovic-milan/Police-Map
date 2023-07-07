package com.example.policemap

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.policemap.data.model.Place

import com.example.policemap.databinding.FragmentPlaceListBinding
import org.w3c.dom.Text

/**
 * [RecyclerView.Adapter] that can display a [PlaceholderItem].
 * TODO: Replace the implementation with code for your data type.
 */
class PlaceRecyclerViewAdapter(
    private val values: List<Place>
) : RecyclerView.Adapter<PlaceRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

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
        holder.timeView.text = item.time.toString()
        holder.expirationView.text = item.expirationTime.toString()
        //TODO: Get users username instead of ID
        holder.userView.text = item.userId.toString()
        //TODO: Use google maps geocoding to display adress here
        holder.addressView.text = "Address goes here"
        holder.ratingView.text = item.rating.toString()

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

//        val idView: TextView = binding.itemNumber
//        val contentView: TextView = binding.content

        override fun toString(): String {
            return super.toString() + " '" + typeView.text + "'"
        }
    }

}