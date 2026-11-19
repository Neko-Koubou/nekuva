package org.nekosukuriputo.nekuva.db

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File
import org.nekosukuriputo.nekuva.core.os.iosAppDataDir

actual class AppContext

actual fun getDatabaseBuilder(ctx: AppContext): RoomDatabase.Builder<NekuvaDatabase> {
    val dbDir = File(iosAppDataDir(), "db").apply { mkdirs() }
    val dbFile = File(dbDir, "nekuva.db")
    return Room.databaseBuilder<NekuvaDatabase>(name = dbFile.absolutePath)
}
