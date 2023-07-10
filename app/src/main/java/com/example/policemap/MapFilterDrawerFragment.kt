package com.example.policemap

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.ToggleButton
import androidx.fragment.app.DialogFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.slider.Slider
import kotlin.math.min

class MapFilterDrawerFragment(
    private val camera: Boolean,
    private val radar: Boolean,
    private val control: Boolean,
    private val patrol: Boolean,
    private val radius: Float,
    private val expired: Boolean,
    private val mineOnly: Boolean
) :
    DialogFragment() {

    private lateinit var cameraToggle: ToggleButton
    private lateinit var radarToggle: ToggleButton
    private lateinit var controlToggle: ToggleButton
    private lateinit var patrolToggle: ToggleButton
    private lateinit var radiusSlider: Slider
    private lateinit var expiredCheckBox: CheckBox
    private lateinit var mineOnlyCheckBox: CheckBox
    private lateinit var applyButton: Button
    private lateinit var clearButton: Button
    private var filterCallback: FilterDrawerListener? = null


    interface FilterDrawerListener {
        fun onFilterApplied(
            cameraOption: Boolean,
            radarOption: Boolean,
            controlOption: Boolean,
            patrolOption: Boolean,
            radius: Float,
            showExpired: Boolean,
            showMineOnly: Boolean,
        )
    }

    fun setRatingDialogCallback(callback: FilterDrawerListener) {
        this.filterCallback = callback
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map_filter_drawer, container, false)
        cameraToggle = view.findViewById(R.id.filter_camera)
        radarToggle = view.findViewById(R.id.filter_radar)
        patrolToggle = view.findViewById(R.id.filter_patrol)
        controlToggle = view.findViewById(R.id.filter_control)
        radiusSlider = view.findViewById(R.id.radius_slider)
        expiredCheckBox = view.findViewById(R.id.expired_checkbox)
        mineOnlyCheckBox = view.findViewById(R.id.mine_checkbox)
        clearButton = view.findViewById(R.id.clear_button)
        applyButton = view.findViewById(R.id.apply_button)
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            dismiss()
        }

        cameraToggle.isChecked = camera
        radarToggle.isChecked = radar
        patrolToggle.isChecked = patrol
        controlToggle.isChecked = control
        radiusSlider.value = radius
        mineOnlyCheckBox.isChecked = mineOnly
        expiredCheckBox.isChecked = expired

        applyButton.setOnClickListener {

            val listener = filterCallback
            listener?.onFilterApplied(
                cameraToggle.isChecked,
                radarToggle.isChecked,
                patrolToggle.isChecked,
                controlToggle.isChecked,
                radiusSlider.value,
                expiredCheckBox.isChecked,
                mineOnlyCheckBox.isChecked
            )
            dismiss()
        }
        clearButton.setOnClickListener {
            onClearFilters()
        }

        return view
    }

    private fun onClearFilters() {
        cameraToggle.isChecked = true
        radarToggle.isChecked = true
        patrolToggle.isChecked = true
        controlToggle.isChecked = true
        radiusSlider.value = 2500F
        expiredCheckBox.isChecked = false
        mineOnlyCheckBox.isChecked = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), R.style.DialogStyle)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
    }

}