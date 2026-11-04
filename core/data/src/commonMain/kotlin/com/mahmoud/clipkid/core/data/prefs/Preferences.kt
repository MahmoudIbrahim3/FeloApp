package com.mahmoud.clipkid.core.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.Path.Companion.toPath

const val PREFERENCES_FILE_NAME = "clipkid.preferences_pb"

internal object PreferenceKeys {
    val OnboardingDone = booleanPreferencesKey("onboarding_done")
    val ActiveChildId = stringPreferencesKey("active_child_id")

    /** Mirrors "a PIN hash exists in SecureStore" so it can be observed. The hash itself never lives here. */
    val PinSet = booleanPreferencesKey("pin_set")
    val CatalogRefreshedAt = longPreferencesKey("catalog_refreshed_at")
}

/** One DataStore per file and process: create it once (Koin single). */
fun createPreferencesDataStore(absolutePath: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { absolutePath.toPath() })

/** A DataStore kept in memory, for tests and previews. */
class InMemoryPreferencesDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
    private val state = MutableStateFlow(initial)
    private val mutex = Mutex()

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        mutex.withLock { transform(state.value).also { state.value = it } }
}
