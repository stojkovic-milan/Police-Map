package com.example.policemap

import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.policemap.data.model.LoggedInUser
import com.example.policemap.ui.login.LoginActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream

class Register : AppCompatActivity() {
    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var buttonReg: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var progressBar: ProgressBar
    private lateinit var loginInstead: TextView

    private lateinit var btnUploadPicture: Button
    private lateinit var pictureBitmap: Bitmap

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
//TODO: DONE?? Add main activity and uncoment after adding
//          var intent = Intent(baseContext, MainActivity::class.java)
//          startActivity(intent)
        }
    }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as Bitmap
                // Save the pictureBitmap and use it for further processing
                pictureBitmap = imageBitmap
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        auth = FirebaseAuth.getInstance()
        editTextEmail = findViewById(R.id.email)
        editTextPassword = findViewById(R.id.password)
        buttonReg = findViewById(R.id.btn_register)
        progressBar = findViewById(R.id.progressBar)
        loginInstead = findViewById(R.id.loginNow)
        btnUploadPicture = findViewById(R.id.btn_upload_picture)

        loginInstead.setOnClickListener {
            var intent = Intent(baseContext, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
        buttonReg.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            var email: String
            var password: String
            email = editTextEmail.text.toString()
            password = editTextPassword.text.toString()

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(baseContext, "Enter email", Toast.LENGTH_SHORT).show()
            } else if (TextUtils.isEmpty(password)) {
                Toast.makeText(baseContext, "Enter password", Toast.LENGTH_SHORT).show()
            } else {
                auth.createUserWithEmailAndPassword("$email@policemap.com", password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            progressBar.visibility = View.GONE
                            uploadInfo(task.result.user!!.uid, email)
                            Toast.makeText(
                                baseContext,
                                "Account created!",
                                Toast.LENGTH_SHORT,
                            ).show()
                            var intent = Intent(baseContext, LoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            progressBar.visibility = View.GONE
                            // If sign in fails, display a message to the user.
//                            Log.w(TAG, "createUserWithEmail:failure", task.exception)
                            Toast.makeText(
                                baseContext,
                                "Authentication failed.",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
            }
        }

        btnUploadPicture.setOnClickListener {
            // Open the camera or gallery to upload/take a picture
            openCameraOrGallery()
        }
    }

    private fun openCameraOrGallery() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            takePictureLauncher.launch(takePictureIntent)
        }
    }

    private fun uploadInfo(id: String, email: String) {
//        val userID: String = auth.currentUser?.uid ?: ""
        val userID: String = id
//        if(userID=="")
//        _actionState.value = ActionState.ActionError("Greska kod upload-a podataka")
        //Upload slike
        var storage = Firebase.storage
        var imageRef: StorageReference? =
            storage.reference.child("users").child(userID)
                .child("${email}.jpg")
        val baos = ByteArrayOutputStream()
        val bitmap = pictureBitmap
        bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()
        val uploadTask = imageRef!!.putBytes(data)

        val urlTask = uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    val user = auth.currentUser
                    user!!.delete()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("SIGNUP", "User account deleted")
                            }
                        }
//                    _actionState.value = ActionState.ActionError("Upload error: ${it.message}")
                }
            }
            imageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val imageUrl = task.result.toString()

                //TODO: Nakon sto napravim model odkomentarisi
                //TODO: Dodaj model za korisnika sa svi potrebni podaci
//                val user = User(name.value, imageUrl)
                val user = LoggedInUser(userID, email)
                val database =
                    Firebase.database("https://police-map-22d2d-default-rtdb.europe-west1.firebasedatabase.app/")
                val userRef = database.reference.child("users").child(userID).setValue(user)
                database.reference.child("emails").child(userID).setValue(email)

                //TODO:Nakon sto dodas User i updateProfile()
//                val profileUpdate = userProfileChangeRequest {
//                    displayName = "${name.value}" photoUri = Uri . parse (imageUrl)
//                }
//
//                auth.currentUser!!.updateProfile(profileUpdate)
//                    .addOnCompleteListener { _actionState.value = ActionState.Success }
            }
        }

    }

    fun takePicture() {
//        if(PermissionHelper.isCameraPermissionGranted(requireContext())){
//            val cameraIntent: Intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//            resultLauncher.launch(cameraIntent)
//        }
//        else {requestPermissionLauncher.launch(Manifest.permission.CAMERA)}
    }

}