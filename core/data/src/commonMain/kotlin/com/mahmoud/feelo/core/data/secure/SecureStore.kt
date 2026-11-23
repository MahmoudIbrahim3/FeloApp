package com.mahmoud.feelo.core.data.secure

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Small secrets (the PIN hash) in platform secure storage:
 * Android Keystore-backed AES-GCM, iOS Keychain (this device only).
 */
interface SecureStore {
    suspend fun get(key: String): String?

    suspend fun put(key: String, value: String)

    suspend fun remove(key: String)
}

/** For tests and previews only. */
class InMemorySecureStore : SecureStore {
    private val mutex = Mutex()
    private val values = mutableMapOf<String, String>()

    override suspend fun get(key: String): String? = mutex.withLock { values[key] }

    override suspend fun put(key: String, value: String) = mutex.withLock { values[key] = value }

    override suspend fun remove(key: String) {
        mutex.withLock { values.remove(key) }
    }
}
