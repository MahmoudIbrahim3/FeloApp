package com.mahmoud.feelo.core.data.db

import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.mahmoud.feelo.core.data.prefs.appDataDirectory

internal actual fun inMemoryDatabaseBuilder(): RoomDatabase.Builder<FeeloDatabase> =
    Room.inMemoryDatabaseBuilder<FeeloDatabase>(factory = { FeeloDatabaseConstructor.initialize() })

internal fun fileDatabaseBuilder(): RoomDatabase.Builder<FeeloDatabase> =
    Room.databaseBuilder<FeeloDatabase>(
        name = "${appDataDirectory()}/${FeeloDatabase.FILE_NAME}",
        factory = { FeeloDatabaseConstructor.initialize() },
    )
