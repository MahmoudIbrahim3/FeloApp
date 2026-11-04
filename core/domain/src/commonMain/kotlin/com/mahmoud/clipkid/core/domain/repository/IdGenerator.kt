package com.mahmoud.clipkid.core.domain.repository

/** Creates unique ids for local records (watch events, profiles). Injected so tests are deterministic. */
fun interface IdGenerator {
    fun newId(): String
}
