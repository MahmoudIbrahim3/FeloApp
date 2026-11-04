package com.mahmoud.clipkid.core.data.db

import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.mahmoud.clipkid.core.data.prefs.appDataDirectory

internal actual fun inMemoryDatabaseBuilder(): RoomDatabase.Builder<ClipKidDatabase> =
    Room.inMemoryDatabaseBuilder<ClipKidDatabase>(factory = { ClipKidDatabaseConstructor.initialize() })

internal fun fileDatabaseBuilder(): RoomDatabase.Builder<ClipKidDatabase> =
    Room.databaseBuilder<ClipKidDatabase>(
        name = "${appDataDirectory()}/${ClipKidDatabase.FILE_NAME}",
        factory = { ClipKidDatabaseConstructor.initialize() },
    )
