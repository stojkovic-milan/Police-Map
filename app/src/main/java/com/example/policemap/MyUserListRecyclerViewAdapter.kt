package com.example.policemap

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.example.policemap.data.model.User

import com.example.policemap.databinding.FragmentUserListBinding

/**
 * [RecyclerView.Adapter] that can display a [User].
 * TODO: Replace the implementation with code for your data type.
 */
class MyUserListRecyclerViewAdapter(
    private val values: MutableList<User>
) : RecyclerView.Adapter<MyUserListRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(
            FragmentUserListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.leaderboardNoView.text = "%d. ".format(position + 1)
        when (position) {
            0 -> holder.leaderboardNoView.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.gold
                )
            )
            1 -> holder.leaderboardNoView.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.silver
                )
            )
            2 -> holder.leaderboardNoView.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.bronze
                )
            )
        }
        holder.idView.text = item.username
        holder.contentView.text = if (item.points != null) item.points.toString() else "0"
        if (item.imageUrl != null)
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .apply(RequestOptions().transform(CenterCrop()))
                .into(holder.profilePictureView)
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(binding: FragmentUserListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val leaderboardNoView: TextView = binding.leaderboardNo
        val idView: TextView = binding.itemNumber
        val contentView: TextView = binding.content
        val profilePictureView: ImageView = binding.profilePic

        override fun toString(): String {
            return super.toString() + " '" + contentView.text + "'"
        }
    }

}