package com.chekurda.secret_pine.main_screen.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.chekurda.common.base_fragment.BasePresenterFragment
import com.chekurda.secret_pine.main_screen.R
import com.chekurda.secret_pine.main_screen.contact.MainScreenFragmentFactory
import com.chekurda.secret_pine.main_screen.utils.PermissionsHelper
import com.chekurda.secret_pine.main_screen.utils.RecordingDeviceHelper

/**
 * Фрагмент главного экрана.
 */
internal class MainScreenFragment : BasePresenterFragment<MainScreenContract.View, MainScreenContract.Presenter>(),
    MainScreenContract.View {

    companion object : MainScreenFragmentFactory {
        override fun createMainScreenFragment(): Fragment = MainScreenFragment()
    }

    override val layoutRes: Int = R.layout.main_screen_fragment

    private var pineModeButton: Button? = null
    private var userModeButton: Button? = null

    private var permissionsHelper: PermissionsHelper? = null
    private var deviceHelper: RecordingDeviceHelper? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        permissionsHelper = PermissionsHelper(requireActivity(), permissions, PERMISSIONS_REQUEST_CODE)
        deviceHelper = RecordingDeviceHelper(requireActivity())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initViews(view: View) {
        pineModeButton = view.findViewById<Button?>(R.id.pine_mode).apply {
            setOnClickListener {
                presenter.onPineModeSelected()
                it.isVisible = false
                userModeButton?.isVisible = false
            }
        }
        userModeButton = view.findViewById<Button?>(R.id.user_mode).apply {
            setOnClickListener {
                presenter.onUserModeSelected()
                it.isVisible = false
                pineModeButton?.isVisible = false
            }
        }
    }

    override fun updateSearchState(isRunning: Boolean) {
        Toast.makeText(context, "updateSearchState = $isRunning", Toast.LENGTH_SHORT).show()
    }

    override fun updateConnectionState(isConnected: Boolean) {
        Toast.makeText(context, "updateConnectionState = $isConnected", Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()
        deviceHelper?.configureDevice(isStartRecording = true)
    }

    override fun onStop() {
        super.onStop()
        deviceHelper?.configureDevice(isStartRecording = false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pineModeButton = null
        userModeButton = null
    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun provideHandler(): Handler = requireView().handler

    override fun provideActivity(): Activity = requireActivity()

    /**
     * DI Press F.
     */
    override fun createPresenter(): MainScreenContract.Presenter = MainScreenPresenterImpl()
}

private val permissions = arrayOf(
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.BLUETOOTH,
    Manifest.permission.BLUETOOTH_ADMIN,
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)
private const val PERMISSIONS_REQUEST_CODE = 102