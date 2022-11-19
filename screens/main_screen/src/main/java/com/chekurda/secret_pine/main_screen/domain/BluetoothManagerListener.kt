package com.chekurda.secret_pine.main_screen.domain

internal interface BluetoothManagerListener {
    fun onConnectionSuccess()
    fun onConnectionCanceled(isError: Boolean)
    fun onSearchStateChanged(isRunning: Boolean)
}