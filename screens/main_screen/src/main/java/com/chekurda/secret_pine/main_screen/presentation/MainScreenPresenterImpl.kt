package com.chekurda.secret_pine.main_screen.presentation

import com.chekurda.common.base_fragment.BasePresenterImpl
import com.chekurda.secret_pine.main_screen.domain.PineBluetoothManager
import com.chekurda.secret_pine.main_screen.domain.UserBluetoothManager

internal class MainScreenPresenterImpl : BasePresenterImpl<MainScreenContract.View>(),
    MainScreenContract.Presenter,
    UserBluetoothManager.ProcessListener {

    private var pineManager: PineBluetoothManager? = null
    private var userManager: UserBluetoothManager? = null

    override fun onPineModeSelected() {
        pineManager = PineBluetoothManager().apply {
            connectionListener = { isConnected ->
                view?.updateConnectionState(isConnected = isConnected)
            }
            startService(view!!.provideActivity(), view!!.provideHandler())
        }
    }

    override fun onUserModeSelected() {
        userManager = UserBluetoothManager().apply {
            init(view!!.provideActivity().applicationContext, view!!.provideHandler())
            processListener = this@MainScreenPresenterImpl
            startSearchPine()
        }
    }

    override fun viewIsStarted() {
        super.viewIsStarted()
        userManager?.startSearchPine()
    }

    override fun viewIsStopped() {
        super.viewIsStopped()
        userManager?.disconnect()
    }

    override fun onSearchStateChanged(isRunning: Boolean) {
        view?.updateSearchState(isRunning)
    }

    override fun onConnectionSuccess() {
        view?.updateConnectionState(isConnected = true)
    }

    override fun onConnectionCanceled(isError: Boolean) {
        view?.updateConnectionState(isConnected = false)
    }

    override fun onDestroy() {
        super.onDestroy()
        pineManager?.release()
        userManager?.release()
    }
}