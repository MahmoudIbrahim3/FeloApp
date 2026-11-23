package com.mahmoud.feelo.core.domain.repository

import kotlinx.coroutines.flow.StateFlow

/** Whether the device has a network connection right now (Android ConnectivityManager / iOS NWPathMonitor). */
interface NetworkMonitor {
    val isOnline: StateFlow<Boolean>
}
