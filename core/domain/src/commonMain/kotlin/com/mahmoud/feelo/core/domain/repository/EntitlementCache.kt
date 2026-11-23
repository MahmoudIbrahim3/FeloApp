package com.mahmoud.feelo.core.domain.repository

import com.mahmoud.feelo.core.model.Entitlement

/**
 * The last subscription status the store reported, kept on the device. Child mode reads only
 * this, so it never has to contact the store.
 */
interface EntitlementCache {
    /** [Entitlement.Free] or [Entitlement.Full]; null when nothing was ever stored. */
    suspend fun read(): Entitlement?

    /** Stores Free or Full. [Entitlement.Unknown] is ignored. */
    suspend fun write(entitlement: Entitlement)
}
