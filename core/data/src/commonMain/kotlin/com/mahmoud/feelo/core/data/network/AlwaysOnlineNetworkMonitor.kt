package com.mahmoud.feelo.core.data.network

import com.mahmoud.feelo.core.domain.repository.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** For tests and previews (inMemoryPlatformDataModule): always online. */
internal object AlwaysOnlineNetworkMonitor : NetworkMonitor {
    override val isOnline: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()
}
