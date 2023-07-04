package com.example.policemap

import android.app.Dialog
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.example.policemap.data.model.Place
import com.example.policemap.data.model.PlaceDb
import com.example.policemap.data.model.PlaceType
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import java.text.SimpleDateFormat
import java.util.*

class AddPlaceDialogFragment : DialogFragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var editTextDateTime: EditText
    private lateinit var buttonAdd: Button
    private lateinit var buttonDiscard: Button
    private lateinit var typeRadioGroup: RadioGroup

    private var selectedDateTime: Calendar = Calendar.getInstance()
    private val db = FirebaseFirestore.getInstance()
//        Firebase.fire("https://police-map-22d2d-default-rtdb.europe-west1.firebasedatabase.app/")

    /** The system calls this to get the DialogFragment's layout, regardless
    of whether it's being displayed as a dialog or an embedded fragment. */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout to use as dialog or embedded fragment
        return inflater.inflate(R.layout.fragment_add_place_dialog, container, false)
    }

    /** The system calls this only when creating the layout in a dialog. */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // The only reason you might override this method when using onCreateView() is
        // to modify any dialog characteristics. For example, the dialog includes a
        // title by default, but your custom layout might not need it. So here you can
        // remove the dialog title, but you must call the superclass to get the Dialog.
//        val dialog = super.onCreateDialog(savedInstanceState)
        val dialog = Dialog(requireContext(), R.style.FloatingDialogTheme)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
    }

    private fun bindViews(view: View) {
        editTextDateTime = view.findViewById(R.id.editTextDateTime)
        buttonAdd = view.findViewById(R.id.buttonSave)
        buttonDiscard = view.findViewById(R.id.buttonDiscard)
        typeRadioGroup = view.findViewById(R.id.radioGroupType)

        editTextDateTime.setOnClickListener {
            showDateTimePicker()
        }
        auth = FirebaseAuth.getInstance()
        buttonAdd.setOnClickListener {
            // Save the selected date and time
            // Do something with the selected date and time
            val selectedRadioButton =
                view.findViewById<RadioButton>(typeRadioGroup.checkedRadioButtonId)

            val time: Date = selectedDateTime.time
            val placeType: PlaceType = PlaceType.valueOf(selectedRadioButton.text.toString())
            val userId: String = auth.currentUser?.uid.toString()
            val placeId: String = UUID.randomUUID().toString()
            val latitude = arguments?.getDouble("lat", 0.0)
            val longitude = arguments?.getDouble("lng", 0.0)
            val location = LatLng(latitude!!, longitude!!)
            val expirationTime: Calendar = Calendar.getInstance()
            if (placeType == PlaceType.Camera) expirationTime.add(
                Calendar.MONTH,
                1
            ) else if (placeType == PlaceType.Control || placeType == PlaceType.Radar) expirationTime.add(
                Calendar.HOUR,
                2
            ) else expirationTime.add(Calendar.MINUTE, 30)
            //TODO: Add empty and null checks here
            val newPlace = PlaceDb(
                placeId,
                location.latitude,
                location.longitude,
                time,
                1,
                placeType,
                userId,
                expirationTime.time
            )
            db.collection("places").document(placeId).set(newPlace)
                .addOnSuccessListener {
                    // Document was successfully written
                    Toast.makeText(
                        requireContext(),
                        "New report added successfully!",
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
                .addOnFailureListener { e ->
                    // Handle any errors that occurred during the write operation
                    Toast.makeText(
                        requireContext(),
                        "Error occurred when adding new report!",
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
            dismiss()
        }
        buttonDiscard.setOnClickListener {
            dismiss()
            //TODO: Add logic for canceling of addition and removing newMarker
        }

        typeRadioGroup.setOnCheckedChangeListener { radioGroup, checkedId ->
            val selectedRadioButton = view.findViewById<RadioButton>(checkedId)
            selectedRadioButton?.let {
                val selectedText = it.text.toString()
                //TODO: Change newMarker icon on selected radio button change
            }
        }
    }

    private fun showDateTimePicker() {
        val now = Calendar.getInstance()

        val dpd = DatePickerDialog.newInstance(
            { _, year, monthOfYear, dayOfMonth ->
                selectedDateTime.set(year, monthOfYear, dayOfMonth)

                val tpd = TimePickerDialog.newInstance(
                    { _, hourOfDay, minute, _ ->
                        selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        selectedDateTime.set(Calendar.MINUTE, minute)

                        // Update the EditText with the selected date and time
                        val pattern = "HH:mm:ss  dd.MM"
                        val simpleDateFormat = SimpleDateFormat(pattern)

//                        val dateTimeString = selectedDateTime.time.toString()
                        val dateTimeString = simpleDateFormat.format(selectedDateTime.time)
                        editTextDateTime.setText(dateTimeString)
                    },
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE),
                    true
                )

                tpd.show(parentFragmentManager, "TimePickerDialog")
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        )

        dpd.show(parentFragmentManager, "DatePickerDialog")
    }
}