package org.nekosukuriputo.nekuva.download.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import nekuva.composeapp.generated.resources.Res
import nekuva.composeapp.generated.resources.downloads
import nekuva.composeapp.generated.resources.download_complete
import org.jetbrains.compose.resources.getString
import org.koin.core.context.GlobalContext
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.Toolkit

private val trayIcon: TrayIcon? by lazy {
    runCatching {
        if (!SystemTray.isSupported()) return@runCatching null
        val image = Toolkit.getDefaultToolkit().createImage(
            object {}.javaClass.classLoader.getResource("nekuva_icon.png"),
        )
        val icon = TrayIcon(image, "Nekuva").apply { isImageAutoSize = true }
        icon.addActionListener {
            org.nekosukuriputo.nekuva.core.events.DesktopEvents.bringToFront()
            org.nekosukuriputo.nekuva.core.nav.DeepLinkBus.requestOpenDownloads()
        }
        SystemTray.getSystemTray().add(icon)
        icon
    }.getOrNull()
}

private suspend fun balloon(caption: String, text: String, type: TrayIcon.MessageType) {
    runCatching { trayIcon?.displayMessage(caption, text, type) }
}

private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
private var observing = false

/** Desktop has no foreground-service / kill model — the engine lives with the app process, but we can show tray notifications. */
actual fun ensureDownloadForeground() {
    if (observing) return
    observing = true
    val dm = GlobalContext.get().get<DownloadManager>()
    scope.launch {
        var lastRunningIds = emptySet<Long>()
        dm.downloads
            .collect { list ->
                val currentRunningIds = list.filter { it.status == DownloadStatus.RUNNING }.map { it.manga.id }.toSet()
                val newlyRunning = currentRunningIds - lastRunningIds
                val finished = list.filter { it.isFinished && lastRunningIds.contains(it.manga.id) }
                
                newlyRunning.forEach { id ->
                    val title = list.firstOrNull { it.manga.id == id }?.manga?.title
                    if (title != null) {
                        balloon(getString(Res.string.downloads), "Downloading $title...", TrayIcon.MessageType.INFO)
                    }
                }
                
                finished.forEach { state ->
                    balloon(getString(Res.string.download_complete), state.manga.title, TrayIcon.MessageType.INFO)
                }
                
                lastRunningIds = currentRunningIds
                if (list.all { it.isFinished }) {
                    observing = false
                    return@collect // stop observing when queue drains, next start will re-trigger
                }
            }
    }
}
