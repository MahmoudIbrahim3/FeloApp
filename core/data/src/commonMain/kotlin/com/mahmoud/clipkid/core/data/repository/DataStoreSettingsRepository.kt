package com.mahmoud.clipkid.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.mahmoud.clipkid.core.data.prefs.PreferenceKeys
import com.mahmoud.clipkid.core.data.secure.PinHasher
import com.mahmoud.clipkid.core.data.secure.SecureStore
import com.mahmoud.clipkid.core.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Plain settings in DataStore; the PIN only as a salted PBKDF2 hash in [SecureStore].
 * If the secure entry is missing (e.g. restored backup without the Keystore key), [hasPin]
 * reports false so the parent can set a new PIN instead of being locked out.
 */
internal class DataStoreSettingsRepository(
    private val prefs: DataStore<Preferences>,
    private val secureStore: SecureStore,
    private val pinHasher: PinHasher,
) : SettingsRepository {

    override val hasPin: Flow<Boolean> = prefs.data
        .map { it[PreferenceKeys.PinSet] == true && secureStore.get(PIN_HASH_KEY) != null }
        .distinctUntilChanged()

    override suspend fun setPin(pin: String) {
        require(pin.isNotEmpty() && pin.all { it in '0'..'9' }) { "PIN must be digits" }
        val hash = withContext(Dispatchers.Default) { pinHasher.hash(pin) }
        secureStore.put(PIN_HASH_KEY, hash)
        prefs.edit { it[PreferenceKeys.PinSet] = true }
    }

    override suspend fun verifyPin(pin: String): Boolean {
        val stored = secureStore.get(PIN_HASH_KEY) ?: return false
        return withContext(Dispatchers.Default) { pinHasher.verify(pin, stored) }
    }

    override val onboardingDone: Flow<Boolean> =
        prefs.data.map { it[PreferenceKeys.OnboardingDone] == true }.distinctUntilChanged()

    override suspend fun setOnboardingDone(done: Boolean) {
        prefs.edit { it[PreferenceKeys.OnboardingDone] = done }
    }

    override val activeChildId: Flow<String?> =
        prefs.data.map { it[PreferenceKeys.ActiveChildId] }.distinctUntilChanged()

    override suspend fun setActiveChildId(id: String?) {
        prefs.edit { if (id == null) it.remove(PreferenceKeys.ActiveChildId) else it[PreferenceKeys.ActiveChildId] = id }
    }

    companion object {
        internal const val PIN_HASH_KEY = "parent_pin_hash"
    }
}
