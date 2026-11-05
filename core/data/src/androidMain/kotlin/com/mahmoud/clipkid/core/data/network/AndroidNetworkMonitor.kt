package com.mahmoud.clipkid.core.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.mahmoud.clipkid.core.domain.repository.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Follows the default network for the whole app lifetime (one Koin single). Needs
 * ACCESS_NETWORK_STATE (a normal permission: no prompt).
 */
internal class AndroidNetworkMonitor(context: Context) : NetworkMonitor {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val state = MutableStateFlow(connectivity.hasInternet(connectivity.activeNetwork))
    override val isOnline: StateFlow<Boolean> = state.asStateFlow()

    init {
        connectivity.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    state.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }

                override fun onLost(network: Network) {
                    state.value = false
                }

                override fun onUnavailable() {
                    state.value = false
                }
            },
        )
    }

    private fun ConnectivityManager.hasInternet(network: Network?): Boolean =
        network != null &&
            getNetworkCapabilities(network)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}
