package com.mahmoud.feelo.core.data.db

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

internal actual fun inMemoryDatabaseBuilder(): RoomDatabase.Builder<FeeloDatabase> =
    Room.inMemoryDatabaseBuilder<FeeloDatabase>()

internal fun fileDatabaseBuilder(context: Context): RoomDatabase.Builder<FeeloDatabase> {
    val appContext = context.applicationContext
    val file = appContext.getDatabasePath(FeeloDatabase.FILE_NAME)
    return Room.databaseBuilder<FeeloDatabase>(appContext, file.absolutePath)
}
