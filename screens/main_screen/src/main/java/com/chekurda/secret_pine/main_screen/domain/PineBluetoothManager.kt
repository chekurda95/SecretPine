package com.chekurda.secret_pine.main_screen.domain

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.util.Log
import com.chekurda.common.storeIn
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.SerialDisposable
import io.reactivex.schedulers.Schedulers
import java.lang.Exception
import java.util.UUID

internal class PineBluetoothManager {

    private var secureUUID = UUID.fromString(PINE_SECURE_UUID)
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private val serviceDisposable = SerialDisposable()
    private val disposer = CompositeDisposable().apply {
        add(serviceDisposable)
    }

    @Volatile
    private var isConnected: Boolean = false

    private var messages: ByteArray = ByteArray(0)
    private var originBluetoothName = ""
    private lateinit var mainHandler: Handler

    var connectionListener: ((Boolean) -> Unit)? = null

    fun startService(context: Context, mainHandler: Handler) {
        bluetoothAdapter.enable()
        val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3000)
        }
        context.startActivity(discoverableIntent)
        openConnectionService()
        originBluetoothName = bluetoothAdapter.name
        bluetoothAdapter.name = PINE_DEVICE_NAME
        this.mainHandler = mainHandler
    }

    @Volatile
    private var serverSocket: BluetoothServerSocket? = null

    private fun openConnectionService() {
        serverSocket?.close()
        serverSocket = null
        Single.fromCallable {
            val serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(PINE_SERVICE_NAME, secureUUID)
            this.serverSocket = serverSocket
            try {
                serverSocket.accept()
            } catch (ex: Exception) {
                serverSocket?.close()
                throw Exception()
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    Log.e("TAGTAG", "onSocketConnected")
                    startPineSocketObserver(it)
                    connectionListener?.invoke(true)
                },
                {
                    Log.e("listen", "${it.message}\n${it.stackTraceToString()}")
                    openConnectionService()
                    connectionListener?.invoke(false)
                }
            )
            .storeIn(serviceDisposable)
    }

    private fun startPineSocketObserver(socket: BluetoothSocket) {
        val thread = object : Thread() {
            override fun run() {
                super.run()
                kotlin.runCatching {
                    isConnected = true
                    while (isConnected) {
                        val checkRemote = socket.remoteDevice
                        if (socket.inputStream.available() != 0) {
                            socket.inputStream.read()
                        } else {
                            socket.outputStream.write(messages)
                        }
                        sleep(1000)
                    }
                }.apply {
                    Log.e("TAGTAG", "onSocketDisconnected")
                    isConnected = false
                    socket.close()
                    serverSocket?.close()
                    serverSocket = null
                    mainHandler.post { connectionListener?.invoke(false) }
                    openConnectionService()
                }
            }
        }
        thread.start()
    }

    fun release() {
        bluetoothAdapter.name = originBluetoothName
        isConnected = false
        serverSocket?.close()
        serverSocket = null
        disposer.dispose()
    }
}

internal const val PINE_SECURE_UUID = "fa87c0d0-afac-11de-8a39-0800200c9a66"
internal const val PINE_DEVICE_NAME = "Secret pine"
internal const val PINE_SERVICE_NAME = "Secret_pine_service"