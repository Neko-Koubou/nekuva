package org.nekosukuriputo.nekuva.download.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import platform.UIKit.UIApplication
import platform.UIKit.UIBackgroundTaskIdentifier
import platform.UIKit.UIBackgroundTaskInvalid
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
private var observing = false
private var backgroundTaskId: UIBackgroundTaskIdentifier = UIBackgroundTaskInvalid
private val notifiedFinishedIds = mutableSetOf<Long>()
private var initializedFinishedIds = false

private class DownloadNotificationDelegate : NSObject(), UNUserNotificationCenterDelegateProtocol {
	override fun userNotificationCenter(
		center: UNUserNotificationCenter,
		willPresentNotification: UNNotification,
		withCompletionHandler: (UNNotificationPresentationOptions) -> Unit,
	) {
		withCompletionHandler(UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionSound)
	}
}

private val notificationDelegate = DownloadNotificationDelegate()

actual fun ensureDownloadForeground() {
	val center = UNUserNotificationCenter.currentNotificationCenter()
	center.delegate = notificationDelegate

	center.requestAuthorizationWithOptions(
		UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
	) { _, _ -> }

	if (observing) {
		updateBackgroundTask()
		return
	}
	observing = true

	scope.launch {
		val dm = runCatching { GlobalContext.get().get<DownloadManager>() }.getOrNull() ?: return@launch
		dm.downloads.collect { list ->
			if (!initializedFinishedIds) {
				initializedFinishedIds = true
				list.forEach { if (it.isFinished) notifiedFinishedIds.add(it.manga.id) }
			}

			list.forEach { state ->
				val id = state.manga.id
				if (state.isFinished && id !in notifiedFinishedIds) {
					notifiedFinishedIds.add(id)
					postFinishNotification(state)
				}
			}

			updateBackgroundTask()
		}
	}
}

private fun updateBackgroundTask() {
	val dm = runCatching { GlobalContext.get().get<DownloadManager>() }.getOrNull() ?: return
	val active = dm.downloads.value.any { it.status == DownloadStatus.RUNNING || it.status == DownloadStatus.QUEUED }

	if (active) {
		if (backgroundTaskId == UIBackgroundTaskInvalid) {
			backgroundTaskId = UIApplication.sharedApplication.beginBackgroundTaskWithName("NekuvaDownload") {
				if (backgroundTaskId != UIBackgroundTaskInvalid) {
					UIApplication.sharedApplication.endBackgroundTask(backgroundTaskId)
					backgroundTaskId = UIBackgroundTaskInvalid
				}
			}
		}
	} else {
		if (backgroundTaskId != UIBackgroundTaskInvalid) {
			UIApplication.sharedApplication.endBackgroundTask(backgroundTaskId)
			backgroundTaskId = UIBackgroundTaskInvalid
		}
	}
}

private fun postFinishNotification(state: DownloadState) {
	val center = UNUserNotificationCenter.currentNotificationCenter()
	val content = UNMutableNotificationContent().apply {
		setTitle(state.manga.title)
		if (state.status == DownloadStatus.COMPLETED) {
			setBody("Download completed")
		} else {
			setBody(state.errorMessage ?: "Download failed")
		}
		setSound(UNNotificationSound.defaultSound)
	}
	val request = UNNotificationRequest.requestWithIdentifier(
		identifier = "download-${state.manga.id}-${state.status.name}",
		content = content,
		trigger = null,
	)
	center.addNotificationRequest(request, null)
}
