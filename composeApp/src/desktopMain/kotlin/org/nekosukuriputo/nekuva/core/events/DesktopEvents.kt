package org.nekosukuriputo.nekuva.core.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object DesktopEvents {
    private val _bringToFront = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val bringToFront = _bringToFront.asSharedFlow()

    private val _navigate = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigate = _navigate.asSharedFlow()

    fun bringToFront() {
        _bringToFront.tryEmit(Unit)
    }

    fun navigateTo(route: String) {
        _navigate.tryEmit(route)
        bringToFront()
    }
}
