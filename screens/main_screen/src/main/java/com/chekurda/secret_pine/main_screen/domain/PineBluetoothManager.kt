package com.chekurda.secret_pine.main_screen.domain

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.chekurda.common.storeIn
import com.chekurda.secret_pine.main_screen.utils.SimpleReceiver
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.SerialDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.lang.Exception
import java.util.UUID

internal class PineBluetoothManager {

    private var secureUUID = UUID.fromString(PINE_LOVER_SECURE_UUID)
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private var deviceBundleSubject = PublishSubject.create<Bundle>()
    private val bluetoothDeviceSubject = deviceBundleSubject.map { extras ->
        extras.getParcelable<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
    }.filter { device -> !device.name.isNullOrBlank() }

    private val searchReceiver = SimpleReceiver(action = BluetoothDevice.ACTION_FOUND) {
        deviceBundleSubject.onNext(it.extras!!)
    }
    private val searchStartReceiver = SimpleReceiver(
        action = BluetoothAdapter.ACTION_DISCOVERY_STARTED,
        isSingleEvent = true
    ) {
        listener?.onSearchStateChanged(isRunning = true)
        context?.let(searchEndReceiver::register)
    }
    private val searchEndReceiver = SimpleReceiver(
        action = BluetoothAdapter.ACTION_DISCOVERY_FINISHED,
        isSingleEvent = true
    ) {
        listener?.onSearchStateChanged(isRunning = false)
        context?.let(searchReceiver::unregister)
    }

    private val deviceListDisposable = SerialDisposable()
    private var connectionDisposable = SerialDisposable()
    private val disposer = CompositeDisposable().apply {
        add(deviceListDisposable)
        add(connectionDisposable)
    }

    @Volatile
    private var isConnected: Boolean = false
    private val isSearching: Boolean
        get() = bluetoothAdapter.isDiscovering

    private var context: Context? = null
    private var mainHandler: Handler? = null

    var listener: BluetoothManagerListener? = null

    fun init(context: Context, mainHandler: Handler) {
        this.context = context
        this.mainHandler = mainHandler
        bluetoothAdapter.enable()
    }

    fun startPineLoverSearching() {
        Log.e("TAGTAG", "startPineLoverSearching")
        val context = context ?: return
        if (isSearching) stopPineLoverSearching()
        subscribeOnDevices()
        searchStartReceiver.register(context)
        searchReceiver.register(context)
        bluetoothAdapter.startDiscovery()
    }

    private fun stopPineLoverSearching() {
        Log.e("TAGTAG", "stopPineLoverSearching")
        val context = context ?: return
        if (!isSearching) return
        deviceListDisposable.set(null)
        bluetoothAdapter.cancelDiscovery()
        searchReceiver.unregister(context)
        searchStartReceiver.unregister(context)
        searchEndReceiver.unregister(context)
        listener?.onSearchStateChanged(isRunning = false)
    }

    private fun connectToPineLover(pineLover: BluetoothDevice) {
        Log.e("TAGTAG", "connectToPineLover")
        if (isSearching) stopPineLoverSearching()
        isConnected = true
        Single.fromCallable {
            val bluetoothDevice = bluetoothAdapter.getRemoteDevice(pineLover.address)
            val socket = bluetoothDevice.createRfcommSocketToServiceRecord(secureUUID)
            try {
                socket.apply { connect() }
            } catch (ex: Exception) {
                socket.close()
                throw Exception()
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    Log.e("TAGTAG", "on socket connected")
                    listener?.onConnectionSuccess()
                    addSocketObserver(it)
                }, {
                    Log.e("TAGTAG", "Socket error ${it.message}\n${it.stackTraceToString()}")
                    startPineLoverSearching()
                }
            ).storeIn(connectionDisposable)
    }

    private fun addSocketObserver(socket: BluetoothSocket) {
        val thread = object : Thread() {
            override fun run() {
                super.run()
                kotlin.runCatching {
                    while (isConnected) {
                        if (socket.inputStream.available() != 0) {
                            // Читаем список сообщений
                        } else {
                            socket.outputStream.write(ByteArray(0))
                        }
                        sleep(1000)
                    }
                }.apply {
                    Log.e("TAGTAG", "onSocketDisconnected")
                    socket.close()
                    isConnected = false
                    mainHandler?.post {
                        startPineLoverSearching()
                        listener?.onConnectionCanceled(isError = true)
                    }
                }
            }
        }
        thread.start()
    }

    fun disconnect() {
        Log.e("TAGTAG", "disconnect")
        if (!isConnected) return
        isConnected = false
        listener?.onConnectionCanceled(isError = false)
    }

    private fun subscribeOnDevices() {
        Log.e("TAGTAG", "subscribeOnDevices")
        bluetoothDeviceSubject.subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { bluetoothDevice ->
                Log.e("TAGTAG", "on some device found")
                if (!isConnected && bluetoothDevice?.name?.contains(PINE_LOVER_DEVICE_NAME) == true) {
                    connectToPineLover(bluetoothDevice)
                }
            }.storeIn(deviceListDisposable)
    }

    fun clear() {
        Log.e("TAGTAG", "clear")
        context = null
        mainHandler = null
    }

    fun release() {
        isConnected = false
        disposer.dispose()
    }
}