package com.mahmoud.clipkid.core.data.network

import com.mahmoud.clipkid.core.domain.repository.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** For tests and previews (inMemoryPlatformDataModule): always online. */
internal object AlwaysOnlineNetworkMonitor : NetworkMonitor {
    override val isOnline: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()
}
