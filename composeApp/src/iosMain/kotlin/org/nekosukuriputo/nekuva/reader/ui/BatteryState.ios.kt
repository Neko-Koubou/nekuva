package org.nekosukuriputo.nekuva.reader.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIDevice

@Composable
actual fun rememberBatteryPercent(): Int? = remember {
    runCatching {
        val device = UIDevice.currentDevice
        device.batteryMonitoringEnabled = true
        val level = device.batteryLevel
        if (level < 0f) null else (level * 100f).toInt().coerceIn(0, 100)
    }.getOrNull()
}
