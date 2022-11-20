package com.chekurda.secret_pine.main_screen.presentation

import com.chekurda.common.base_fragment.BasePresenterImpl
import com.chekurda.secret_pine.main_screen.domain.BluetoothManagerListener
import com.chekurda.secret_pine.main_screen.domain.UserBluetoothManager
import com.chekurda.secret_pine.main_screen.domain.PineBluetoothManager

internal class MainScreenPresenterImpl : BasePresenterImpl<MainScreenContract.View>(),
    MainScreenContract.Presenter,
    BluetoothManagerListener {

    private var userManager: UserBluetoothManager? = null
    private var pineManager: PineBluetoothManager? = null
    private var isConnected: Boolean = false

    override fun attachView(view: MainScreenContract.View) {
        super.attachView(view)
        userManager?.init(view.provideActivity().applicationContext, view.provideHandler())
        pineManager?.init(view.provideActivity().applicationContext, view.provideHandler())
    }

    override fun detachView() {
        super.detachView()
        userManager?.clear()
        pineManager?.clear()
    }

    override fun onPineModeSelected() {
        pineManager = PineBluetoothManager().apply {
            init(view!!.provideActivity().applicationContext, view!!.provideHandler())
            listener = this@MainScreenPresenterImpl
            startPineLoverSearching()
        }
    }

    override fun onUserModeSelected() {
        userManager = UserBluetoothManager().apply {
            init(view!!.provideActivity().applicationContext, view!!.provideHandler())
            listener = this@MainScreenPresenterImpl
            onMessageListChanged = { messageList ->
                view?.updateMessageList(messageList)
            }
            startPineDetectService()
        }
    }

    override fun viewIsStarted() {
        super.viewIsStarted()
        if (!isConnected) {
            userManager?.startPineDetectService()
            pineManager?.startPineLoverSearching()
        }
    }

    override fun viewIsStopped() {
        super.viewIsStopped()
        if (isConnected) {
            userManager?.disconnect()
            pineManager?.disconnect()
        }
    }

    override fun onSearchStateChanged(isRunning: Boolean) {
        view?.updateSearchState(isRunning)
        if (isStarted && !isRunning && !isConnected) {
            pineManager?.startPineLoverSearching()
        }
    }

    override fun onConnectionSuccess() {
        isConnected = true
        view?.updateConnectionState(isConnected = true)
    }

    override fun onConnectionCanceled(isError: Boolean) {
        isConnected = false
        view?.updateConnectionState(isConnected = false)
        if (isStarted) {
            userManager?.startPineDetectService()
            pineManager?.startPineLoverSearching()
        }
    }

    override fun sendMessage(text: String) {
        userManager?.sendMessage(text)
    }

    override fun onDestroy() {
        super.onDestroy()
        pineManager?.release()
        userManager?.release()
    }
}