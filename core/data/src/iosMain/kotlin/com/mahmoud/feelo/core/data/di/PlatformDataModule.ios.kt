package com.mahmoud.feelo.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.mahmoud.feelo.core.data.db.FeeloDatabase
import com.mahmoud.feelo.core.data.db.buildFeelo
import com.mahmoud.feelo.core.data.db.fileDatabaseBuilder
import com.mahmoud.feelo.core.data.network.IosNetworkMonitor
import com.mahmoud.feelo.core.data.prefs.PREFERENCES_FILE_NAME
import com.mahmoud.feelo.core.data.prefs.appDataDirectory
import com.mahmoud.feelo.core.data.prefs.createPreferencesDataStore
import com.mahmoud.feelo.core.data.secure.KeychainSecureStore
import com.mahmoud.feelo.core.data.secure.SecureStore
import com.mahmoud.feelo.core.domain.repository.NetworkMonitor
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single<FeeloDatabase> { fileDatabaseBuilder().buildFeelo() }
    single<DataStore<Preferences>> { createPreferencesDataStore("${appDataDirectory()}/$PREFERENCES_FILE_NAME") }
    single<SecureStore> { KeychainSecureStore() }
    single<NetworkMonitor> { IosNetworkMonitor() }
}
