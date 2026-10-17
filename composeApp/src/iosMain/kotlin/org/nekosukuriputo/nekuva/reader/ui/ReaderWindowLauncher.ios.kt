package org.nekosukuriputo.nekuva.reader.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberReaderWindowLauncher(): ReaderWindowLauncher = remember {
    ReaderWindowLauncher { false }
}
