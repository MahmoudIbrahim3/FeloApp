package com.mahmoud.feelo.core.data.secure

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.io.encoding.Base64

/**
 * Values are encrypted with an AES-256-GCM key that never leaves the Android Keystore, then kept
 * in a private SharedPreferences file. (security-crypto's EncryptedSharedPreferences is
 * deprecated, so this uses the Keystore directly.)
 *
 * The key is not part of backups: a restored ciphertext cannot be decrypted, is dropped, and
 * reads as missing.
 */
internal class KeystoreSecureStore(context: Context) : SecureStore {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
    private val mutex = Mutex()

    override suspend fun get(key: String): String? = locked {
        val stored = prefs.getString(key, null) ?: return@locked null
        try {
            decrypt(key, stored)
        } catch (_: GeneralSecurityException) {
            prefs.edit().remove(key).commit()
            null
        } catch (_: IllegalArgumentException) {
            prefs.edit().remove(key).commit()
            null
        }
    }

    override suspend fun put(key: String, value: String) = locked {
        check(prefs.edit().putString(key, encrypt(key, value)).commit()) { "Could not save secure value" }
    }

    override suspend fun remove(key: String) = locked {
        prefs.edit().remove(key).commit()
        Unit
    }

    private suspend fun <T> locked(block: () -> T): T = withContext(Dispatchers.IO) { mutex.withLock { block() } }

    private fun encrypt(key: String, value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        // Binds the ciphertext to its entry name.
        cipher.updateAAD(key.encodeToByteArray())
        val ciphertext = cipher.doFinal(value.encodeToByteArray())
        return Base64.encode(cipher.iv) + SEPARATOR + Base64.encode(ciphertext)
    }

    private fun decrypt(key: String, stored: String): String {
        val (iv, ciphertext) = stored.split(SEPARATOR).also { require(it.size == 2) }.map { Base64.decode(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(key.encodeToByteArray())
        return cipher.doFinal(ciphertext).decodeToString()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val spec = KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "feelo.securestore.v1"
        const val PREFS_FILE = "feelo_secure"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val SEPARATOR = ":"
    }
}
