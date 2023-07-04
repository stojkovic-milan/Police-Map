package com.example.policemap

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment

class RatingDialogFragment : DialogFragment() {
    private var startRating: Int = 0
    private var increment: Int = 0
    private lateinit var placeId: String

    interface RatingDialogCallback {
        fun onRatingSubmitted(placeId: String, rating: Int)
    }

    private var ratingCallback: RatingDialogCallback? = null

    //
//    override fun onAttach(context: Context) {
//        super.onAttach(context)
//        // Ensure that the host activity implements the RatingChangeListener interface
//        if (context is RatingChangeListener) {
//            ratingChangeListener = context
//        }
//    }
    fun setRatingDialogCallback(callback: RatingDialogCallback) {
        this.ratingCallback = callback
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout to use as dialog or embedded fragment
        return inflater.inflate(R.layout.fragment_rating_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
    }

    private fun bindViews(view: View) {
        startRating = arguments?.getInt("rating", 1)!!
        placeId = arguments?.getString("id", "")!!

        val textViewRating = view.findViewById<TextView>(R.id.text_view_rating)
        val buttonMinus = view.findViewById<Button>(R.id.button_minus)
        val buttonPlus = view.findViewById<Button>(R.id.button_plus)
        val buttonSubmit = view.findViewById<Button>(R.id.button_ok)
        val buttonCancel = view.findViewById<Button>(R.id.button_cancel)

        textViewRating.text = startRating.toString()

        buttonMinus.setOnClickListener {
            increment = -1
            textViewRating.text = (startRating + increment).toString()
        }

        buttonPlus.setOnClickListener {
            increment = 1
            textViewRating.text = (startRating + increment).toString()
        }

        buttonSubmit.setOnClickListener {
            ratingCallback?.onRatingSubmitted(placeId, increment)
            dismiss()
        }
        buttonCancel.setOnClickListener { dismiss() }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        val inflater = LayoutInflater.from(requireContext())
//        val dialogView = inflater.inflate(R.layout.fragment_rating_dialog, null)
        val dialog = Dialog(requireContext(), R.style.FloatingDialogTheme)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }
}