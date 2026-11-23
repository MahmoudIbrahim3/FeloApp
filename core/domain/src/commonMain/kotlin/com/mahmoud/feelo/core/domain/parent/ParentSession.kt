package com.mahmoud.feelo.core.domain.parent

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Whether parent mode is open. Opened only after the gate, closed when the parent leaves parent
 * mode, when the app goes to the background, or after [idleTimeout] without a touch.
 *
 * [touch] is cheap (it only records the time), so it can be called for every pointer event.
 *
 * Store purchase sheets: Google Play's billing screen or a bank's 3-D Secure page can send the app
 * to the background, and paying can take longer than [idleTimeout]. While a purchase or restore
 * started from the paywall is running ([holdForStoreFlow]), parent mode stays open; the idle
 * countdown restarts when it ends.
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
    private val storeFlows = MutableStateFlow(0)

    fun unlock() {
        lastActivity.value = clock.now()
        _isUnlocked.value = true
        idleWatch?.cancel()
        idleWatch = scope.launch {
            while (_isUnlocked.value) {
                if (storeFlows.value > 0) lastActivity.value = clock.now()
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

    /** The app went to the background: closes parent mode, unless a store sheet is open. */
    fun onAppStopped() {
        if (storeFlows.value == 0) lock()
    }

    /** Runs a purchase or restore. Only meaningful while unlocked (the paywall is behind the gate). */
    suspend fun <T> holdForStoreFlow(block: suspend () -> T): T {
        storeFlows.update { it + 1 }
        try {
            return block()
        } finally {
            storeFlows.update { it - 1 }
            touch()
        }
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
