package com.mahmoud.clipkid.core.data.secure

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PinHasherTest {

    private val fast = PinHasher(iterations = 1_000)

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun pbkdf2MatchesRfc7914TestVector() {
        val derived = pbkdf2HmacSha256("passwd", "salt".encodeToByteArray(), iterations = 1, keyLengthBytes = 64)
        assertEquals(
            "55ac046e56e3089fec1691c22544b605f94185216dde0465e68b9d57c20dacbc" +
                "49ca9cccf179b645991664b39d77ef317c71b845b1e30bd509112041d3a19783",
            derived.toHexString(),
        )
    }

    @Test
    fun verifiesAHashMadeOnAnotherPlatform() {
        // Python: hashlib.pbkdf2_hmac('sha256', b'1234', b'clipkid-salt-01', 100000, 32), base64-encoded.
        val stored = "pbkdf2-sha256$100000\$Y2xpcGtpZC1zYWx0LTAx\$4SpXhJ6TbUcOtdLh22DzhwekQyelmV7pnqDDoh9VC9U="
        assertTrue(PinHasher().verify("1234", stored))
        assertFalse(PinHasher().verify("1235", stored))
    }

    @Test
    fun roundTripAndWrongPin() {
        val stored = fast.hash("2580")
        assertTrue(fast.verify("2580", stored))
        assertFalse(fast.verify("2581", stored))
        assertFalse(fast.verify("", stored))
    }

    @Test
    fun storedValueIsSaltedAndNeverContainsThePin() {
        val a = fast.hash("2580")
        val b = fast.hash("2580")
        assertNotEquals(a, b)
        assertFalse("2580" in a)
        assertTrue(a.startsWith("pbkdf2-sha256$1000$"))
    }

    @Test
    fun defaultUsesOneHundredThousandIterations() {
        assertTrue(PinHasher().hash("0000").startsWith("pbkdf2-sha256$100000$"))
    }

    @Test
    fun malformedStoredValuesNeverVerify() {
        listOf("", "1234", "sha256\$1\$abc\$def", "pbkdf2-sha256\$x\$AAAA\$AAAA", "pbkdf2-sha256$1$!!!$!!!")
            .forEach { assertFalse(fast.verify("1234", it), it) }
    }

    @Test
    fun secureRandomBytesHaveTheRequestedSize() {
        val a = secureRandomBytes(16)
        assertEquals(16, a.size)
        assertFalse(a.contentEquals(secureRandomBytes(16)))
        assertContentEquals(ByteArray(0), secureRandomBytes(0))
    }
}
