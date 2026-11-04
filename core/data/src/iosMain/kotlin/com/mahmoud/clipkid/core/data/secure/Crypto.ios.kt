package com.mahmoud.clipkid.core.data.secure

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CCKeyDerivationPBKDF
import platform.CoreCrypto.kCCPBKDF2
import platform.CoreCrypto.kCCPRFHmacAlgSHA256
import platform.CoreCrypto.kCCSuccess
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault

@OptIn(ExperimentalForeignApi::class, ExperimentalUnsignedTypes::class)
internal actual fun pbkdf2HmacSha256(password: String, salt: ByteArray, iterations: Int, keyLengthBytes: Int): ByteArray {
    require(salt.isNotEmpty() && keyLengthBytes > 0)
    val derived = UByteArray(keyLengthBytes)
    val saltBytes = salt.asUByteArray()
    val status = derived.usePinned { out ->
        saltBytes.usePinned { s ->
            CCKeyDerivationPBKDF(
                algorithm = kCCPBKDF2,
                password = password,
                passwordLen = password.encodeToByteArray().size.convert(),
                salt = s.addressOf(0),
                saltLen = salt.size.convert(),
                prf = kCCPRFHmacAlgSHA256,
                rounds = iterations.convert(),
                derivedKey = out.addressOf(0),
                derivedKeyLen = keyLengthBytes.convert(),
            )
        }
    }
    check(status == kCCSuccess) { "PBKDF2 failed: $status" }
    return derived.asByteArray()
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun secureRandomBytes(size: Int): ByteArray {
    val bytes = ByteArray(size)
    if (size == 0) return bytes
    val status = bytes.usePinned { SecRandomCopyBytes(kSecRandomDefault, size.convert(), it.addressOf(0)) }
    check(status == errSecSuccess) { "SecRandomCopyBytes failed: $status" }
    return bytes
}
