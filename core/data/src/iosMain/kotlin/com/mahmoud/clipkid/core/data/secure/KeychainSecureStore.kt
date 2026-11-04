package com.mahmoud.clipkid.core.data.secure

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.posix.memcpy

/** Generic-password Keychain items, readable only on this device and only while it is unlocked. */
@OptIn(ExperimentalForeignApi::class)
internal class KeychainSecureStore(private val service: String = SERVICE) : SecureStore {
    private val mutex = Mutex()

    override suspend fun get(key: String): String? = mutex.withLock { read(key) }

    override suspend fun put(key: String, value: String) = mutex.withLock {
        delete(key)
        val dataRef = CFBridgingRetain(value.encodeToByteArray().toNSData())
        try {
            val status = withQuery(
                key,
                kSecValueData to dataRef,
                kSecAttrAccessible to kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
            ) { SecItemAdd(it, null) }
            check(status == errSecSuccess) { "Keychain add failed: $status" }
        } finally {
            CFBridgingRelease(dataRef)
        }
    }

    override suspend fun remove(key: String) = mutex.withLock { delete(key) }

    private fun read(key: String): String? = memScoped {
        val result = alloc<CFTypeRefVar>()
        val status = withQuery(key, kSecReturnData to kCFBooleanTrue, kSecMatchLimit to kSecMatchLimitOne) {
            SecItemCopyMatching(it, result.ptr)
        }
        if (status != errSecSuccess) return@memScoped null
        (CFBridgingRelease(result.value) as? NSData)?.toByteArray()?.decodeToString()
    }

    private fun delete(key: String) {
        withQuery(key) { SecItemDelete(it) }
    }

    private inline fun <T> withQuery(
        key: String,
        vararg extra: Pair<CFStringRef?, CFTypeRef?>,
        block: (CFDictionaryRef?) -> T,
    ): T {
        val serviceRef = CFBridgingRetain(service)
        val accountRef = CFBridgingRetain(key)
        val query = CFDictionaryCreateMutable(
            null,
            (3 + extra.size).convert(),
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        )
        try {
            CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionaryAddValue(query, kSecAttrService, serviceRef)
            CFDictionaryAddValue(query, kSecAttrAccount, accountRef)
            extra.forEach { (k, v) -> CFDictionaryAddValue(query, k, v) }
            return block(query)
        } finally {
            CFRelease(query)
            CFBridgingRelease(serviceRef)
            CFBridgingRelease(accountRef)
        }
    }

    private companion object {
        const val SERVICE = "app.clipkid.securestore"
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData =
    if (isEmpty()) NSData() else usePinned { NSData.create(bytes = it.addressOf(0), length = size.convert()) }

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val out = ByteArray(size)
    if (size > 0) out.usePinned { memcpy(it.addressOf(0), bytes, length) }
    return out
}
