package com.chekurda.secret_pine.main_screen.domain

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.chekurda.common.storeIn
import com.chekurda.secret_pine.main_screen.data.DeviceInfo
import com.chekurda.secret_pine.main_screen.utils.SimpleReceiver
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.SerialDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.lang.Exception
import java.util.UUID

internal class UserBluetoothManager {

    interface ProcessListener {
        fun onConnectionSuccess()
        fun onConnectionCanceled(isError: Boolean)
        fun onSearchStateChanged(isRunning: Boolean)
    }

    var processListener: ProcessListener? = null

    private var secureUUID = UUID.fromString(PINE_SECURE_UUID)
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
        processListener?.onSearchStateChanged(isRunning = true)
        searchEndReceiver.register(appContext)
    }
    private val searchEndReceiver = SimpleReceiver(
        action = BluetoothAdapter.ACTION_DISCOVERY_FINISHED,
        isSingleEvent = true
    ) {
        processListener?.onSearchStateChanged(isRunning = false)
        searchReceiver.unregister(appContext)
    }

    private val deviceListDisposable = SerialDisposable()
    private val disposer = CompositeDisposable().apply {
        add(deviceListDisposable)
    }

    @Volatile
    private var isConnected: Boolean = false
    private val isSearching: Boolean
        get() = bluetoothAdapter.isDiscovering

    private lateinit var appContext: Context
    private lateinit var mainHandler: Handler

    fun init(context: Context, mainHandler: Handler) {
        appContext = context
        this.mainHandler = mainHandler
    }

    fun startSearchPine() {
        if (isSearching) stopSearchDevices()
        subscribeOnDevices()
        searchStartReceiver.register(appContext)
        searchReceiver.register(appContext)
        bluetoothAdapter.startDiscovery()
    }

    private fun stopSearchDevices() {
        if (!isSearching) return
        deviceListDisposable.set(null)
        bluetoothAdapter.cancelDiscovery()
        searchReceiver.unregister(appContext)
        searchStartReceiver.unregister(appContext)
        searchEndReceiver.unregister(appContext)
        processListener?.onSearchStateChanged(isRunning = false)
    }

    private fun connect(pine: BluetoothDevice) {
        if (isSearching) stopSearchDevices()
        isConnected = true
        Single.fromCallable {
            val bluetoothDevice = bluetoothAdapter.getRemoteDevice(pine.address)
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
                    Log.e("TAGTAG", "onSocketConnected")
                    processListener?.onConnectionSuccess()
                    addSocketObserver(it)
                }, {
                    Log.e("connect", "${it.message}\n${it.stackTraceToString()}")
                    startSearchPine()
                }
            ).storeIn(disposer)
    }

    private fun addSocketObserver(socket: BluetoothSocket) {
        val thread = object : Thread() {
            override fun run() {
                super.run()
                kotlin.runCatching {
                    while (isConnected) {
                        val pineDevice = socket.remoteDevice
                        if (socket.inputStream.available() != 0) {
                            // Читаем список сообщений
                        } else {
                            socket.outputStream.write(ByteArray(0))
                        }
                    }
                }.apply {
                    socket.close()
                    isConnected = false
                    mainHandler.post { processListener?.onConnectionCanceled(isError = true) }
                }
            }
        }
        thread.start()
    }

    fun disconnect() {
        if (!isConnected) return
        isConnected = false
        processListener?.onConnectionCanceled(isError = false)
    }

    private fun subscribeOnDevices() {
        bluetoothDeviceSubject.subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { bluetoothDevice ->
                if (!isConnected && bluetoothDevice?.name == PINE_DEVICE_NAME) {
                    connect(bluetoothDevice)
                }
            }.storeIn(deviceListDisposable)
    }

    fun release() {
        isConnected = false
        disposer.dispose()
    }
}