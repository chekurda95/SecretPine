package com.chekurda.secret_pine.main_screen.domain

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.util.Log
import com.chekurda.common.storeIn
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.SerialDisposable
import io.reactivex.schedulers.Schedulers
import java.lang.Exception
import java.util.UUID
import java.util.concurrent.TimeUnit

internal class UserBluetoothManager {

    private var secureUUID = UUID.fromString(PINE_LOVER_SECURE_UUID)
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private val serviceDisposable = SerialDisposable()
    private val discoverableDisposable = SerialDisposable()
    private val disposer = CompositeDisposable().apply {
        add(serviceDisposable)
        add(discoverableDisposable)
    }

    @Volatile
    private var isConnected: Boolean = false
    private var isDiscoverable: Boolean = false

    private var messages: ByteArray = ByteArray(0)
    private var originBluetoothName = ""

    private var context: Context? = null
    private var mainHandler: Handler? = null

    var listener: BluetoothManagerListener? = null

    fun init(context: Context, mainHandler: Handler) {
        this.context = context
        this.mainHandler = mainHandler
    }

    fun startPineDetectService() {
        if (!isDiscoverable) makeDiscoverable()
        openPineSearchingService()
        originBluetoothName = bluetoothAdapter.name
        bluetoothAdapter.name = "%s %s".format(PINE_LOVER_DEVICE_NAME, bluetoothAdapter.name)
    }

    private fun makeDiscoverable() {
        val context = context ?: return
        val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
        }
        context.startActivity(discoverableIntent)
        isDiscoverable = true
        Observable.timer(120, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                isDiscoverable = false
                if (!isConnected) makeDiscoverable()
            }.storeIn(discoverableDisposable)
    }

    @Volatile
    private var serverSocket: BluetoothServerSocket? = null

    private fun openPineSearchingService() {
        if (context == null) return
        Log.e("TAGTAG", "openPineSearchingService")
        serverSocket?.close()
        serverSocket = null
        Single.fromCallable {
            val serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(PINE_SERVICE_NAME, secureUUID)
            this.serverSocket = serverSocket
            var socket: BluetoothSocket? = null
            try {
                socket = serverSocket.accept()
            } catch (ex: Exception) {
                serverSocket?.close()
            }
            socket!!
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    Log.e("TAGTAG", "onSocketConnected")
                    startPineSocketObserver(it)
                    listener?.onConnectionSuccess()
                },
                {
                    Log.e("TAGTAG", "openPineSearchingService error ${it.message}\n${it.stackTraceToString()}")
                    openPineSearchingService()
                    listener?.onConnectionCanceled(isError = true)
                }
            )
            .storeIn(serviceDisposable)
    }

    private fun startPineSocketObserver(pineSocket: BluetoothSocket) {
        val thread = object : Thread() {
            override fun run() {
                super.run()
                kotlin.runCatching {
                    isConnected = true
                    while (isConnected) {
                        if (pineSocket.inputStream.available() != 0) {
                            pineSocket.inputStream.read()
                        } else {
                            pineSocket.outputStream.write(messages)
                        }
                        sleep(1000)
                    }
                }.apply {
                    Log.e("TAGTAG", "onSocketDisconnected")
                    isConnected = false
                    pineSocket.close()
                    serverSocket?.close()
                    serverSocket = null
                    mainHandler?.post { listener?.onConnectionCanceled(isError = false) }
                    openPineSearchingService()
                }
            }
        }
        thread.start()
    }

    fun disconnect() {
        Log.e("TAGTAG", "disconnect")
        discoverableDisposable.set(null)
        bluetoothAdapter.name = originBluetoothName
        if (!isConnected) return
        isConnected = false
        listener?.onConnectionCanceled(isError = false)
    }

    fun clear() {
        Log.e("TAGTAG", "clear")
        context = null
        mainHandler = null
        discoverableDisposable.set(null)
        bluetoothAdapter.name = originBluetoothName
    }

    fun release() {
        bluetoothAdapter.name = originBluetoothName
        isConnected = false
        serverSocket?.close()
        serverSocket = null
        disposer.dispose()
    }
}

internal const val PINE_LOVER_SECURE_UUID = "fa87c0d0-afac-11de-8a39-0800200c9a66"
internal const val PINE_LOVER_DEVICE_NAME = "Pine lover"
internal const val PINE_SERVICE_NAME = "Secret_pine_service"