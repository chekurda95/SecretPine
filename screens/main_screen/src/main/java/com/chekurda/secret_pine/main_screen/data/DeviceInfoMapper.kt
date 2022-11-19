package com.chekurda.secret_pine.main_screen.data

import android.net.wifi.p2p.WifiP2pDevice

internal val WifiP2pDevice.deviceInfo: DeviceInfo
    get() = DeviceInfo(
        address = deviceAddress,
        name = deviceName
    )