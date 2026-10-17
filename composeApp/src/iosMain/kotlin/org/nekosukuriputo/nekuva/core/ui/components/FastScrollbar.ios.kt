package org.nekosukuriputo.nekuva.core.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun FastScrollbar(
    state: LazyGridState,
    modifier: Modifier,
) {
    // No-op on iOS: LazyVerticalGrid uses native touch scrolling physics
}

@Composable
actual fun FastScrollbar(
    state: LazyListState,
    modifier: Modifier,
) {
    // No-op on iOS
}
