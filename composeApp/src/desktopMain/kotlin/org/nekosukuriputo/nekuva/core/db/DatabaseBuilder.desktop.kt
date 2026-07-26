package org.nekosukuriputo.nekuva.core.db

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

actual fun getDatabaseBuilder(): RoomDatabase.Builder<MangaDatabase> {
    val dbDir = File(File(System.getProperty("user.home"), ".nekuva"), "db").apply { mkdirs() }
    val dbFile = File(dbDir, "nekuva-db.db")
    return Room.databaseBuilder<MangaDatabase>(
        name = dbFile.absolutePath,
    )
}
