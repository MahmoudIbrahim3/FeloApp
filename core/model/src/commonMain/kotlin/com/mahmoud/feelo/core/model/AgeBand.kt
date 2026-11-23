package com.mahmoud.feelo.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Age bands a collection, a video or a child profile belongs to (Google Play's Families groups). */
@Serializable
enum class AgeBand {
    @SerialName("3-5") A3_5,
    @SerialName("6-8") A6_8,
    @SerialName("9-12") A9_12,
}
