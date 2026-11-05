package com.mahmoud.clipkid.feature.parent.paywall

/** Which store sells the subscription on this platform (for the auto-renew text). */
internal enum class PlatformStore { GooglePlay, AppStore }

internal expect val platformStore: PlatformStore
