package com.mahmoud.clipkid.core.data.secure

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

internal actual fun pbkdf2HmacSha256(password: String, salt: ByteArray, iterations: Int, keyLengthBytes: Int): ByteArray {
    val spec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLengthBytes * 8)
    return try {
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    } finally {
        spec.clearPassword()
    }
}

private val random = SecureRandom()

internal actual fun secureRandomBytes(size: Int): ByteArray = ByteArray(size).also(random::nextBytes)
