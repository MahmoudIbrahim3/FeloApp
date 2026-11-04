package com.mahmoud.clipkid.core.domain.parent

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Whether parent mode is open. Opened only after the gate, closed when the parent leaves parent
 * mode, when the app goes to the background, or after [idleTimeout] without a touch.
 *
 * [touch] is cheap (it only records the time), so it can be called for every pointer event.
 */
class ParentSession(
    private val clock: Clock,
    private val scope: CoroutineScope,
    private val idleTimeout: Duration = IDLE_TIMEOUT,
) {
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val lastActivity = MutableStateFlow(Instant.DISTANT_PAST)
    private var idleWatch: Job? = null

    fun unlock() {
        lastActivity.value = clock.now()
        _isUnlocked.value = true
        idleWatch?.cancel()
        idleWatch = scope.launch {
            while (_isUnlocked.value) {
                val idle = clock.now() - lastActivity.value
                if (idle >= idleTimeout) {
                    lock()
                    break
                }
                delay(idleTimeout - idle)
            }
        }
    }

    /** Any parent interaction: restarts the idle countdown. Ignored while locked. */
    fun touch() {
        if (_isUnlocked.value) lastActivity.value = clock.now()
    }

    fun lock() {
        _isUnlocked.value = false
        idleWatch?.cancel()
        idleWatch = null
    }

    companion object {
        val IDLE_TIMEOUT: Duration = 2.minutes
    }
}
