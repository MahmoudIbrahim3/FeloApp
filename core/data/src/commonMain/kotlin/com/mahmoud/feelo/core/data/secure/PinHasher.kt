package com.mahmoud.feelo.core.data.secure

import kotlin.io.encoding.Base64

/**
 * PIN hashing: PBKDF2-HMAC-SHA256 with a random 16-byte salt (exceeds the "salt + SHA-256"
 * minimum: a 4-digit PIN has only 10 000 values, so slow hashing matters).
 *
 * Stored format: `pbkdf2-sha256$<iterations>$<salt base64>$<hash base64>`, so iterations can
 * be raised later without breaking existing PINs.
 */
class PinHasher(private val iterations: Int = DEFAULT_ITERATIONS) {

    fun hash(pin: String): String {
        val salt = secureRandomBytes(SALT_BYTES)
        val hash = pbkdf2HmacSha256(pin, salt, iterations, HASH_BYTES)
        return listOf(PREFIX, iterations.toString(), Base64.encode(salt), Base64.encode(hash)).joinToString("$")
    }

    fun verify(pin: String, stored: String): Boolean {
        val parts = stored.split('$')
        if (parts.size != 4 || parts[0] != PREFIX) return false
        val rounds = parts[1].toIntOrNull()?.takeIf { it > 0 } ?: return false
        val salt = runCatching { Base64.decode(parts[2]) }.getOrNull() ?: return false
        val expected = runCatching { Base64.decode(parts[3]) }.getOrNull() ?: return false
        val actual = pbkdf2HmacSha256(pin, salt, rounds, expected.size)
        return constantTimeEquals(actual, expected)
    }

    companion object {
        const val DEFAULT_ITERATIONS = 100_000
        private const val PREFIX = "pbkdf2-sha256"
        private const val SALT_BYTES = 16
        private const val HASH_BYTES = 32

        internal fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
            if (a.size != b.size) return false
            var diff = 0
            for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
            return diff == 0
        }
    }
}

/** PBKDF2-HMAC-SHA256 over the UTF-8 bytes of [password]. Android: javax.crypto, iOS: CommonCrypto. */
internal expect fun pbkdf2HmacSha256(password: String, salt: ByteArray, iterations: Int, keyLengthBytes: Int): ByteArray

/** Cryptographically secure random bytes. Android: SecureRandom, iOS: SecRandomCopyBytes. */
internal expect fun secureRandomBytes(size: Int): ByteArray
