package org.nekosukuriputo.nekuva.download.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Scale
import coil3.size.Size
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import nekuva.composeapp.generated.resources.Res
import nekuva.composeapp.generated.resources.cancel
import nekuva.composeapp.generated.resources.cancel_all
import nekuva.composeapp.generated.resources.downloads
import nekuva.composeapp.generated.resources.pause
import nekuva.composeapp.generated.resources.resume
import org.jetbrains.compose.resources.getString
import org.koin.core.context.GlobalContext
import org.nekosukuriputo.nekuva.MainActivity
import org.nekosukuriputo.nekuva.download.domain.ChapterDownloadStatus
import org.nekosukuriputo.nekuva.download.domain.DownloadManager
import org.nekosukuriputo.nekuva.download.domain.DownloadState
import org.nekosukuriputo.nekuva.download.domain.DownloadStatus

/**
 * Foreground service backing the in-process [DownloadManager] (Doki's WorkManager foreground
 * notification). Observes the download list, posts an ongoing progress notification while any download
 * is RUNNING/QUEUED — keeping the process alive — and stops itself once the queue drains.
 */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observing = false
    private var channelName: String = "Downloads"
    // Action labels resolved once (localized) for the notification buttons.
    private var pauseLabel = "Pause"
    private var resumeLabel = "Resume"
    private var cancelLabel = "Cancel"
    private var cancelAllLabel = "Cancel All"
    private val covers = mutableMapOf<Long, Bitmap>()
    private val fetchingCovers = mutableSetOf<Long>()
    private var lastActiveIds = emptySet<Long>()
    private var lastStatuses = emptyMap<Long, DownloadStatus>()
    private var notifiedFinishedIds = mutableSetOf<Long>()
    private var initializedFinishedIds = false
    private var lastUpdate = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // Resolve the channel name + action labels once (localized); a one-time in-memory resource read.
        runCatching {
            runBlocking {
                channelName = getString(Res.string.downloads)
                pauseLabel = getString(Res.string.pause)
                resumeLabel = getString(Res.string.resume)
                cancelLabel = getString(Res.string.cancel)
                cancelAllLabel = getString(Res.string.cancel_all)
            }
        }
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle the notification action buttons (pause/resume/cancel all).
        val dm = GlobalContext.get().get<DownloadManager>()
        // Must call startForeground promptly after startForegroundService().
        startForegroundCompat(buildNotification(null, 0, hasRunning = false, cover = null))
        
        scope.launch {
            if (dm.downloads.value.isEmpty()) {
                runCatching { dm.restoreQueue() }
            }
            when (intent?.action) {
                ACTION_PAUSE_ALL -> dm.pauseAll()
                ACTION_RESUME_ALL -> dm.resumeAll()
                ACTION_CANCEL_ALL -> dm.cancelAll()
                ACTION_PAUSE -> intent.getStringExtra(EXTRA_MANGA_ID)?.let { dm.pause(it) }
                ACTION_RESUME -> intent.getStringExtra(EXTRA_MANGA_ID)?.let { dm.resume(it) }
                ACTION_CANCEL -> intent.getStringExtra(EXTRA_MANGA_ID)?.let { dm.cancel(it) }
            }
            
            if (!observing) {
                observing = true
                observe(dm)
            }
        }
        return START_NOT_STICKY
    }

    private fun observe(dm: DownloadManager) {
        scope.launch {
            dm.downloads.collect { list ->
                if (!initializedFinishedIds) {
                    initializedFinishedIds = true
                    list.forEach { if (it.isFinished) notifiedFinishedIds.add(it.manga.id) }
                }
                
                list.forEach { state ->
                    val id = state.manga.id
                    if (state.isFinished && id !in notifiedFinishedIds) {
                        notifiedFinishedIds.add(id)
                        val cover = covers[id]
                        val notification = buildFinalNotification(state, cover)
                        NotificationManagerCompat.from(this@DownloadService).notify(id.toInt(), notification)
                    }
                }

                val active = list.filter { !it.isFinished }
                if (active.isEmpty()) {
                    stopForegroundCompat()
                    stopSelf()
                    lastActiveIds = emptySet()
                    covers.clear()
                    return@collect
                }
                
                val allCurrentIds = list.map { it.manga.id }.toSet()
                val removedIds = lastActiveIds - allCurrentIds
                removedIds.forEach { id -> NotificationManagerCompat.from(this@DownloadService).cancel(id.toInt()) }
                lastActiveIds = active.map { it.manga.id }.toSet()

                val hasRunning = active.any { it.status == DownloadStatus.RUNNING }

                val currentStatuses = active.associate { it.manga.id to it.status }
                val hasStatusChange = lastStatuses != currentStatuses

                val now = System.currentTimeMillis()
                val shouldUpdate = (now - lastUpdate) > 500L
                val isDone = active.isEmpty()
                
                if (shouldUpdate || isDone || hasStatusChange) {
                    lastUpdate = now
                    lastStatuses = currentStatuses
                    startForegroundCompat(buildSummaryNotification(active.size, hasRunning))
                    
                    active.forEach { state ->
                        val cover = covers[state.manga.id]
                        val isRunning = state.status == DownloadStatus.RUNNING || state.status == DownloadStatus.QUEUED
                        val notification = buildMangaNotification(state, isRunning, cover)
                        NotificationManagerCompat.from(this@DownloadService).notify(state.manga.id.toInt(), notification)
                        
                        if (cover == null && !state.manga.coverUrl.isNullOrEmpty() && !fetchingCovers.contains(state.manga.id)) {
                            fetchingCovers.add(state.manga.id)
                            fetchCoverAndUpdate(state, isRunning)
                        }
                    }
                }
            }
        }
    }

    private fun fetchCoverAndUpdate(state: DownloadState, isRunning: Boolean) {
        scope.launch(Dispatchers.IO) {
            try {
                val coil = SingletonImageLoader.get(this@DownloadService)
                val request = ImageRequest.Builder(this@DownloadService)
                    .data(state.manga.coverUrl)
                    .allowHardware(false)
                    .size(Size(256, 256))
                    .scale(Scale.FILL)
                    .build()
                val result = coil.execute(request)
                val drawable = result.image?.asDrawable(resources)
                val bitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap ?: drawable?.toBitmap()
                if (bitmap != null) {
                    covers[state.manga.id] = bitmap
                    val notification = buildMangaNotification(state, isRunning, bitmap)
                    NotificationManagerCompat.from(this@DownloadService).notify(state.manga.id.toInt(), notification)
                }
            } catch (e: Exception) {
                // Ignore image load failures
            } finally {
                fetchingCovers.remove(state.manga.id)
            }
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW)
            NotificationManagerCompat.from(this).createNotificationChannel(channel)
        }
    }


    private fun contentIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .setAction("org.nekosukuriputo.nekuva.OPEN_DOWNLOADS")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun buildSummaryNotification(activeCount: Int, hasRunning: Boolean): Notification {
        val title = channelName
        val contentText = if (hasRunning) "Downloading $activeCount items" else "Paused $activeCount items"
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent())
            .setGroup(GROUP_ID)
            .setGroupSummary(true)
            
        if (hasRunning) {
            builder.addAction(android.R.drawable.ic_media_pause, pauseLabel, actionIntent(ACTION_PAUSE_ALL))
        } else {
            builder.addAction(android.R.drawable.ic_media_play, resumeLabel, actionIntent(ACTION_RESUME_ALL))
        }
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, cancelAllLabel, actionIntent(ACTION_CANCEL_ALL))
        
        return builder.build()
    }

    private fun buildMangaNotification(state: DownloadState, isRunning: Boolean, cover: Bitmap?): Notification {
        val intent = Intent(this, MainActivity::class.java)
            .setAction("org.nekosukuriputo.nekuva.OPEN_DOWNLOADS")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val percent = (state.percent * 100).toInt()
        val title = state.manga.title
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pending)
            .setGroup(GROUP_ID)
            
        if (cover != null) {
            builder.setLargeIcon(cover)
        }
        
        var contentText = "$percent%"
        if (state.eta > 0) {
            val etaString = DateUtils.getRelativeTimeSpanString(
                System.currentTimeMillis() + state.eta,
                System.currentTimeMillis(),
                DateUtils.SECOND_IN_MILLIS
            )
            contentText += " • $etaString"
        }
        builder.setContentText(contentText)
        
        if (isRunning) {
            builder.addAction(android.R.drawable.ic_media_pause, pauseLabel, actionIntent(ACTION_PAUSE, state.id))
        } else {
            builder.addAction(android.R.drawable.ic_media_play, resumeLabel, actionIntent(ACTION_RESUME, state.id))
        }
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, cancelLabel, actionIntent(ACTION_CANCEL, state.id))
        
        if (state.max > 0) {
            builder.setProgress(state.max, state.progress, state.isIndeterminate)
        } else {
            builder.setProgress(0, 0, true)
        }
        return builder.build()
    }

    private fun buildFinalNotification(state: DownloadState, cover: Bitmap?): Notification {
        val intent = Intent(this, MainActivity::class.java)
            .setAction("org.nekosukuriputo.nekuva.OPEN_DOWNLOADS")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val title = state.manga.title
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setOngoing(false) // So user can swipe it away
            .setContentIntent(pending)
            .setGroup(GROUP_ID)
            
        if (cover != null) {
            builder.setLargeIcon(cover)
        }
        
        val contentText = when (state.status) {
            DownloadStatus.COMPLETED -> "Downloaded" // Will be translated if we add proper string logic later
            DownloadStatus.FAILED -> "Failed"
            DownloadStatus.CANCELLED -> "Cancelled"
            else -> ""
        }
        builder.setContentText(contentText)
        
        return builder.build()
    }

    private fun buildNotification(state: DownloadState?, activeCount: Int, hasRunning: Boolean, cover: Bitmap?): Notification {
        // Fallback or deprecated usage
        return buildSummaryNotification(activeCount, hasRunning)
    }

    private fun actionIntent(action: String, mangaId: String? = null): PendingIntent {
        val intent = Intent(this, DownloadService::class.java).setAction(action)
        if (mangaId != null) {
            intent.putExtra(EXTRA_MANGA_ID, mangaId)
        }
        return PendingIntent.getService(
            this,
            (action + mangaId).hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun startForegroundCompat(notification: Notification) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val CHANNEL_ID = "downloads"
        const val NOTIFICATION_ID = 0x444C // non-zero (required for a foreground notification)
        const val ACTION_PAUSE_ALL = "org.nekosukuriputo.nekuva.download.PAUSE_ALL"
        const val ACTION_RESUME_ALL = "org.nekosukuriputo.nekuva.download.RESUME_ALL"
        const val ACTION_CANCEL_ALL = "org.nekosukuriputo.nekuva.download.CANCEL_ALL"
        const val ACTION_PAUSE = "org.nekosukuriputo.nekuva.download.PAUSE"
        const val ACTION_RESUME = "org.nekosukuriputo.nekuva.download.RESUME"
        const val ACTION_CANCEL = "org.nekosukuriputo.nekuva.download.CANCEL"
        const val EXTRA_MANGA_ID = "manga_id"
        const val GROUP_ID = "downloads_group"
    }
}
