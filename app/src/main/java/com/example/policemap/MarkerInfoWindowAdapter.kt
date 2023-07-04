package com.example.policemap

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.example.policemap.data.model.Place
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import java.text.SimpleDateFormat
import java.util.*

class MarkerInfoWindowAdapter(
    private val context: Context
) : GoogleMap.InfoWindowAdapter {
    override fun getInfoContents(marker: Marker?): View? {
        // 1. Get tag
        val place = marker?.tag as? Place ?: return null

        // 2. Inflate view and set title, address, and rating
        val view = LayoutInflater.from(context).inflate(
            R.layout.marker_info_contents, null
        )
        view.findViewById<TextView>(
            R.id.text_view_title
        ).text = place.placeType.toString()
        val pattern = "HH:mm:ss"
        val simpleDateFormat = SimpleDateFormat(pattern)

        val diff: Long = Date().time - place.time!!.time
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        val dateOutput: String =
            if (days == 0L && hours == 0L) {
                if (minutes == 0L)
                    "Just now"
                else
                    "%d minutes ago".format(minutes)
            } else simpleDateFormat.format(
                place.time
            )
        view.findViewById<TextView>(
            R.id.text_view_address
        ).text = dateOutput
        view.findViewById<TextView>(
            R.id.text_view_rating
        ).text = " %d ".format(place.rating)

        return view
    }

    override fun getInfoWindow(marker: Marker?): View? {
        // Return null to indicate that the
        // default window (white bubble) should be used
        return null
    }
}