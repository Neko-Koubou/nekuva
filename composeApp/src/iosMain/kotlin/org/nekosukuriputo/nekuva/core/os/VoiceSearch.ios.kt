package org.nekosukuriputo.nekuva.core.os

import androidx.compose.runtime.Composable

@Composable
actual fun rememberVoiceSearchLauncher(onResult: (String) -> Unit): (() -> Unit)? = null
