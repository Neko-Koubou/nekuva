@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.nekosukuriputo.nekuva.core.security

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics

actual fun isBiometricAvailable(): Boolean = runCatching {
    val context = LAContext()
    context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, error = null)
}.getOrDefault(false)

actual suspend fun authenticateBiometric(title: String, subtitle: String): Boolean {
    if (!isBiometricAvailable()) return false
    return suspendCancellableCoroutine { cont ->
        val context = LAContext()
        val reason = subtitle.ifBlank { title.ifBlank { "Unlock Nekuva" } }
        context.evaluatePolicy(
            policy = LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            localizedReason = reason,
        ) { success, _ ->
            if (cont.isActive) {
                cont.resume(success)
            }
        }
    }
}
