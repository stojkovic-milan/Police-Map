package com.example.policemap

//import android.R
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.example.policemap.ui.login.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MainFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MainFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var button: Button
    private lateinit var mapButton: Button
    private lateinit var textView: TextView
    private lateinit var user: FirebaseUser
    private lateinit var profileImage: ImageView
    private lateinit var storageRef: StorageReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        auth = FirebaseAuth.getInstance()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var rootView = inflater.inflate(R.layout.fragment_main, container, false)

        button = rootView.findViewById(R.id.logout)
        mapButton = rootView.findViewById(R.id.mapButton)
        textView = rootView.findViewById(R.id.user_details)
        profileImage = rootView.findViewById(R.id.profile_image)

        if (auth.currentUser == null) {
            var intent = Intent(activity, MainActivity::class.java)
            startActivity(intent)
            activity?.finish()
        } else {
            user = auth.currentUser!!
            textView.text = "Welcome, ${user.email?.replace("@policemap.com", "")}"

            //Show profile picture
            val currentUser = FirebaseAuth.getInstance().currentUser
            storageRef = FirebaseStorage.getInstance().reference
                .child("users")
                .child(currentUser?.uid ?: "")
                .child("${user.email?.replace("@policemap.com", "")}.jpg")

            // Load the image into the ImageView
            loadProfileImage()

        }
        button.setOnClickListener {
            auth.signOut()
            var intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
            activity?.finish()
        }
        mapButton.setOnClickListener {
            val destFragment = MapsFragment()
//            parentFragmentManager.beginTransaction()
//                .replace(ActivityMainBinding.inflate(layoutInflater).root.id,destFragment).commit()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(
                    requireActivity().findViewById<CoordinatorLayout>(R.id.fragment_container).id,
                    destFragment
                )
                .commit()
        }

        return rootView
    }

    private fun loadProfileImage() {
//        storageRef.downloadUrl.addOnSuccessListener { uri ->
//            val imageUrl = uri.toString()
//
//            // Use Glide to load the image into the ImageView
//            Glide.with(this)
//                .load(imageUrl)
//                .apply(RequestOptions().transform(CenterCrop()))
//                .into(profileImage)
//        }.addOnFailureListener { exception ->
//            // Handle any errors that occurred while retrieving the download URL
//        }
        // Use Glide to load the image into the ImageView
        Glide.with(this)
            .load(auth.currentUser!!.photoUrl)
            .apply(RequestOptions().transform(CenterCrop()))
            .into(profileImage)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MainFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MainFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}