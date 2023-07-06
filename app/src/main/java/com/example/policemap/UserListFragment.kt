package com.example.policemap

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.policemap.data.model.User
import com.google.firebase.database.*

/**
 * A fragment representing a list of users.
 */
class UserListFragment : Fragment() {

    private lateinit var databaseRef: DatabaseReference
    private lateinit var userAdapter: MyUserListRecyclerViewAdapter
    private var users: MutableList<User> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        databaseRef = FirebaseDatabase.getInstance().getReference("users")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_list_list, container, false)

        // Set up the RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.list)
        recyclerView.layoutManager = LinearLayoutManager(context)
        userAdapter = MyUserListRecyclerViewAdapter(users)
        recyclerView.adapter = userAdapter

        return view
    }

    override fun onStart() {
        super.onStart()

        // Add a ValueEventListener to fetch and observe changes to the user data
        databaseRef.orderByChild("points").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                users.clear()
                for (userSnapshot in snapshot.children.reversed()) {
                    val user = userSnapshot.getValue(User::class.java)
                    user?.let {
                        users.add(it)
                    }
                }
                userAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })
    }

    companion object {
        @JvmStatic
        fun newInstance() = UserListFragment()
    }
}
