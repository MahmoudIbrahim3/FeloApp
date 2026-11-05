package com.mahmoud.clipkid.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.mahmoud.clipkid.core.data.db.ClipKidDatabase
import com.mahmoud.clipkid.core.data.db.buildClipKid
import com.mahmoud.clipkid.core.data.db.fileDatabaseBuilder
import com.mahmoud.clipkid.core.data.network.IosNetworkMonitor
import com.mahmoud.clipkid.core.data.prefs.PREFERENCES_FILE_NAME
import com.mahmoud.clipkid.core.data.prefs.appDataDirectory
import com.mahmoud.clipkid.core.data.prefs.createPreferencesDataStore
import com.mahmoud.clipkid.core.data.secure.KeychainSecureStore
import com.mahmoud.clipkid.core.data.secure.SecureStore
import com.mahmoud.clipkid.core.domain.repository.NetworkMonitor
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single<ClipKidDatabase> { fileDatabaseBuilder().buildClipKid() }
    single<DataStore<Preferences>> { createPreferencesDataStore("${appDataDirectory()}/$PREFERENCES_FILE_NAME") }
    single<SecureStore> { KeychainSecureStore() }
    single<NetworkMonitor> { IosNetworkMonitor() }
}
