package com.mahmoud.clipkid.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Age bands a collection, a video or a child profile belongs to. */
@Serializable
enum class AgeBand {
    @SerialName("2-3") A2_3,
    @SerialName("4-6") A4_6,
    @SerialName("7-9") A7_9,
    @SerialName("10-12") A10_12,
}
