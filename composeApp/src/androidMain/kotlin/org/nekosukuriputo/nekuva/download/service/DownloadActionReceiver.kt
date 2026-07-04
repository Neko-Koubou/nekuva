package org.nekosukuriputo.nekuva.download.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.nekosukuriputo.nekuva.download.domain.DownloadManager

class DownloadActionReceiver : BroadcastReceiver(), KoinComponent {
    private val dm: DownloadManager by inject()

    override fun onReceive(context: Context, intent: Intent) {
        
        when (intent.action) {
            DownloadService.ACTION_PAUSE_ALL -> dm.pauseAll()
            DownloadService.ACTION_RESUME_ALL -> dm.resumeAll()
            DownloadService.ACTION_CANCEL_ALL -> dm.cancelAll()
            DownloadService.ACTION_PAUSE -> intent.getStringExtra(DownloadService.EXTRA_MANGA_ID)?.let { dm.pause(it) }
            DownloadService.ACTION_RESUME -> intent.getStringExtra(DownloadService.EXTRA_MANGA_ID)?.let { dm.resume(it) }
            DownloadService.ACTION_CANCEL -> intent.getStringExtra(DownloadService.EXTRA_MANGA_ID)?.let { dm.cancel(it) }
        }
        
        // Ensure the service is woken up to observe the state and update the notification!
        val serviceIntent = Intent(context, DownloadService::class.java)
        try {
            androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            android.util.Log.e("DownloadActionReceiver", "Failed to start service", e)
        }
    }
}
