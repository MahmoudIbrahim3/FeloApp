package com.mahmoud.clipkid.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.mahmoud.clipkid.core.data.db.ClipKidDatabase
import com.mahmoud.clipkid.core.data.db.buildClipKid
import com.mahmoud.clipkid.core.data.db.fileDatabaseBuilder
import com.mahmoud.clipkid.core.data.prefs.PREFERENCES_FILE_NAME
import com.mahmoud.clipkid.core.data.prefs.createPreferencesDataStore
import com.mahmoud.clipkid.core.data.secure.KeystoreSecureStore
import com.mahmoud.clipkid.core.data.secure.SecureStore
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single<ClipKidDatabase> { fileDatabaseBuilder(androidContext()).buildClipKid() }
    single<DataStore<Preferences>> {
        createPreferencesDataStore(androidContext().filesDir.resolve(PREFERENCES_FILE_NAME).absolutePath)
    }
    single<SecureStore> { KeystoreSecureStore(androidContext()) }
}
