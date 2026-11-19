package org.nekosukuriputo.nekuva.reader.data

actual suspend fun pickPageSaveDir(): String? =
    org.nekosukuriputo.nekuva.download.ui.dialog.pickMangaDirectory()
