package com.mahmoud.feelo.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.mahmoud.feelo.core.data.prefs.PreferenceKeys
import com.mahmoud.feelo.core.domain.repository.EntitlementCache
import com.mahmoud.feelo.core.model.BillingStore
import com.mahmoud.feelo.core.model.Entitlement
import kotlin.time.Instant
import kotlinx.coroutines.flow.first

/**
 * The last subscription status in DataStore, so child mode never needs the store. Only what the
 * dashboard and the paywall show is kept: no store or RevenueCat ids.
 */
internal class DataStoreEntitlementCache(
    private val prefs: DataStore<Preferences>,
) : EntitlementCache {

    override suspend fun read(): Entitlement? {
        val p = prefs.data.first()
        val full = p[PreferenceKeys.EntitlementFull] ?: return null
        if (!full) return Entitlement.Free
        return Entitlement.Full(
            expiresAt = p[PreferenceKeys.EntitlementExpiresAt]?.let(Instant::fromEpochMilliseconds),
            willRenew = p[PreferenceKeys.EntitlementWillRenew] ?: false,
            isTrial = p[PreferenceKeys.EntitlementTrial] ?: false,
            store = p[PreferenceKeys.EntitlementStore]
                ?.let { name -> BillingStore.entries.firstOrNull { it.name == name } }
                ?: BillingStore.Other,
            managementUrl = p[PreferenceKeys.EntitlementManagementUrl],
        )
    }

    override suspend fun write(entitlement: Entitlement) {
        when (entitlement) {
            Entitlement.Unknown -> return
            Entitlement.Free -> prefs.edit {
                it[PreferenceKeys.EntitlementFull] = false
                it.removeDetails()
            }
            is Entitlement.Full -> prefs.edit {
                it[PreferenceKeys.EntitlementFull] = true
                it.removeDetails()
                entitlement.expiresAt?.let { at -> it[PreferenceKeys.EntitlementExpiresAt] = at.toEpochMilliseconds() }
                it[PreferenceKeys.EntitlementWillRenew] = entitlement.willRenew
                it[PreferenceKeys.EntitlementTrial] = entitlement.isTrial
                it[PreferenceKeys.EntitlementStore] = entitlement.store.name
                entitlement.managementUrl?.let { url -> it[PreferenceKeys.EntitlementManagementUrl] = url }
            }
        }
    }

    private fun MutablePreferences.removeDetails() {
        remove(PreferenceKeys.EntitlementExpiresAt)
        remove(PreferenceKeys.EntitlementWillRenew)
        remove(PreferenceKeys.EntitlementTrial)
        remove(PreferenceKeys.EntitlementStore)
        remove(PreferenceKeys.EntitlementManagementUrl)
    }
}
