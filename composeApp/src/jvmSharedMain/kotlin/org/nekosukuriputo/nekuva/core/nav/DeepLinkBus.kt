package org.nekosukuriputo.nekuva.core.nav

import kotlinx.coroutines.flow.MutableSharedFlow

object DeepLinkBus {
    val openMangaId = MutableSharedFlow<Long>(replay = 1, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)
    fun requestOpenManga(id: Long) { openMangaId.tryEmit(id) }
    fun consume() { openMangaId.resetReplayCache() }

    val openSourceName = MutableSharedFlow<String>(replay = 1, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)
    fun requestOpenSource(name: String) { openSourceName.tryEmit(name) }
    fun consumeSource() { openSourceName.resetReplayCache() }

    val openDownloads = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST)
    fun requestOpenDownloads() { openDownloads.tryEmit(Unit) }
    fun consumeDownloads() { openDownloads.resetReplayCache() }
}
