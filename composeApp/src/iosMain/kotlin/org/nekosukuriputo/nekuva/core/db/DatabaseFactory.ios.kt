package org.nekosukuriputo.nekuva.core.db

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File
import org.nekosukuriputo.nekuva.core.os.iosAppDataDir

actual fun getDatabaseBuilder(): RoomDatabase.Builder<MangaDatabase> {
    val dbDir = File(iosAppDataDir(), "db").apply { mkdirs() }
    val dbFile = File(dbDir, "nekuva_manga.db")
    return Room.databaseBuilder<MangaDatabase>(
        name = dbFile.absolutePath,
    )
}
