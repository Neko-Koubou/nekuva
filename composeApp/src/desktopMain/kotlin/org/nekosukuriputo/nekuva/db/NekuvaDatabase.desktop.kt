package org.nekosukuriputo.nekuva.db

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

actual class AppContext

actual fun getDatabaseBuilder(ctx: AppContext): RoomDatabase.Builder<NekuvaDatabase> {
    val dbDir = File(File(System.getProperty("user.home"), ".nekuva"), "db").apply { mkdirs() }
    val dbFile = File(dbDir, "nekuva.db")
    return Room.databaseBuilder<NekuvaDatabase>(name = dbFile.absolutePath)
}
