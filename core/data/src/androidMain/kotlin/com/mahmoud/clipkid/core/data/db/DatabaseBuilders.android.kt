package com.mahmoud.clipkid.core.data.db

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

internal actual fun inMemoryDatabaseBuilder(): RoomDatabase.Builder<ClipKidDatabase> =
    Room.inMemoryDatabaseBuilder<ClipKidDatabase>()

internal fun fileDatabaseBuilder(context: Context): RoomDatabase.Builder<ClipKidDatabase> {
    val appContext = context.applicationContext
    val file = appContext.getDatabasePath(ClipKidDatabase.FILE_NAME)
    return Room.databaseBuilder<ClipKidDatabase>(appContext, file.absolutePath)
}
