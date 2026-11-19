package org.nekosukuriputo.nekuva.backups.domain

actual suspend fun notifyBackupStart(isRestore: Boolean) = Unit

actual suspend fun notifyBackupFinish(isRestore: Boolean, success: Boolean) = Unit
