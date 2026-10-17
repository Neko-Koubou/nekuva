@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package org.nekosukuriputo.nekuva.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.backhandler.BackHandler

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
