package com.chekurda.secret_pine.main_screen.domain

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.util.Log
import com.chekurda.common.storeIn
import com.chekurda.secret_pine.main_screen.data.Message
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.SerialDisposable
import io.reactivex.schedulers.Schedulers
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
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

    private var outputStream: ObjectOutputStream? = null
    private val messageList: MutableList<Message> = mutableListOf()

    private var originBluetoothName = ""

    private var context: Context? = null
    private var mainHandler: Handler? = null

    var listener: BluetoothManagerListener? = null
    var onMessageListChanged: ((List<Message>) -> Unit)? = null

    fun init(context: Context, mainHandler: Handler) {
        this.context = context
        this.mainHandler = mainHandler
    }

    fun startPineDetectService() {
        if (isConnected) return
        if (!isDiscoverable) makeDiscoverable()
        openPineSearchingService()
        prepareDeviceName()
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
        this.outputStream = ObjectOutputStream(pineSocket.outputStream)
        val inputStream = ObjectInputStream(pineSocket.inputStream)
        val thread = object : Thread() {

            override fun run() {
                super.run()
                kotlin.runCatching {
                    isConnected = true
                    val connectionCheckArray = ByteArray(0)
                    while (isConnected) {
                        if (pineSocket.inputStream.available() != 0) {
                            when (val obj = inputStream.readObject()) {
                                is List<*> -> (obj as? List<Message>)?.also { inputMessageList ->
                                    mainHandler?.post {
                                        messageList.clear()
                                        val mappedList = inputMessageList.apply { map { it.isOutcome = it.senderName == originBluetoothName }}
                                        messageList.addAll(mappedList)
                                        onMessageListChanged?.invoke(messageList)
                                    }
                                }
                                is Message -> {
                                    mainHandler?.post {
                                        messageList.add(obj.apply { isOutcome = senderName == originBluetoothName })
                                        onMessageListChanged?.invoke(messageList)
                                    }
                                }
                                else -> Unit
                            }
                            Log.e("TAGTAG", "success write")
                        } else {
                            pineSocket.outputStream.write(connectionCheckArray)
                        }
                    }
                }.apply {
                    Log.e("TAGTAG", "onSocketDisconnected")
                    isConnected = false

                    pineSocket.close()
                    serverSocket?.close()
                    serverSocket = null
                    mainHandler?.post {
                        this@UserBluetoothManager.outputStream = null
                        listener?.onConnectionCanceled(isError = false)
                    }
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

    fun sendMessage(text: String) {
        val outputStream = outputStream ?: return
        Completable.fromCallable {
            val message = Message(
                uuid = UUID.randomUUID(),
                senderName = originBluetoothName,
                text = text
            )
            outputStream.writeObject(message)
        }.subscribeOn(Schedulers.io())
            .subscribe(
                { Log.e("TAGTAG", "onMessage sent") },
                { Log.e("TAGTAG", "onMessage sent error $it") }
            )
            .storeIn(disposer)
    }

    private fun prepareDeviceName() {
        originBluetoothName = bluetoothAdapter.name
        if (!originBluetoothName.contains(PINE_LOVER_DEVICE_NAME)) {
            bluetoothAdapter.name = "%s %s".format(PINE_LOVER_DEVICE_NAME, bluetoothAdapter.name)
        }
    }
}

internal const val PINE_LOVER_SECURE_UUID = "fa87c0d0-afac-11de-8a39-0800200c9a66"
internal const val PINE_LOVER_DEVICE_NAME = "Pine lover"
internal const val PINE_SERVICE_NAME = "Secret_pine_service"