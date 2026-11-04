package com.mahmoud.clipkid.feature.parent.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahmoud.clipkid.core.domain.repository.ProfileRepository
import com.mahmoud.clipkid.core.domain.repository.SettingsRepository
import com.mahmoud.clipkid.core.domain.usecase.GetCollectionsForAgeUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetDailyQuotaUseCase
import com.mahmoud.clipkid.core.domain.usecase.GrantExtraVideosUseCase
import com.mahmoud.clipkid.core.domain.usecase.UpdateChildSettingsUseCase
import com.mahmoud.clipkid.core.model.ChildProfile
import com.mahmoud.clipkid.core.model.Collection
import com.mahmoud.clipkid.core.model.DailyLimit
import com.mahmoud.clipkid.core.model.DailyQuota
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CollectionToggle(val collection: Collection, val allowed: Boolean) {
    /** Premium collections need a subscription (billing phase): shown with a star, cannot be on. */
    val locked: Boolean get() = collection.isPremium
}

sealed interface DashboardUiState {
    data object Loading : DashboardUiState

    /** No child yet (should not happen after onboarding): only "add child" is offered. */
    data object NoChildren : DashboardUiState

    data class Content(
        val children: List<ChildProfile>,
        /** The active child: the one child mode shows, and the one the cards below edit. */
        val selected: ChildProfile,
        val quota: DailyQuota,
        val collections: List<CollectionToggle>,
    ) : DashboardUiState
}

sealed interface DashboardEvent {
    data class SelectChild(val childId: String) : DashboardEvent
    data object AddChildClicked : DashboardEvent
    data class LimitChanged(val limit: DailyLimit) : DashboardEvent
    data class CollectionToggled(val collectionId: String, val allowed: Boolean) : DashboardEvent

    /** +1 / +3 for today only. */
    data class GrantExtra(val videos: Int) : DashboardEvent
    data object ActivityClicked : DashboardEvent
    data object SubscriptionClicked : DashboardEvent

    /** Lock button: back to child mode. */
    data object LockClicked : DashboardEvent
}

sealed interface DashboardEffect {
    data object OpenAddChild : DashboardEffect
    data class OpenActivity(val childId: String) : DashboardEffect
    data object OpenSubscription : DashboardEffect
    data object Lock : DashboardEffect
}

/** Parent dashboard (Parent.png). Every change is saved at once, so child Home follows it. */
class DashboardViewModel(
    private val profiles: ProfileRepository,
    private val settings: SettingsRepository,
    private val getQuota: GetDailyQuotaUseCase,
    private val getCollectionsForAge: GetCollectionsForAgeUseCase,
    private val updateChild: UpdateChildSettingsUseCase,
    private val grantExtras: GrantExtraVideosUseCase,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<DashboardUiState> = combine(
        profiles.observeProfiles(),
        settings.activeChildId,
    ) { all, activeId -> all to activeId }
        .flatMapLatest { (all, activeId) ->
            val children = all
            val selected = children.firstOrNull { it.id == activeId } ?: children.firstOrNull()
            if (selected == null) {
                flowOf(DashboardUiState.NoChildren)
            } else {
                // No (or a deleted) active child: the first one becomes active, so child mode has one.
                if (selected.id != activeId) viewModelScope.launch { settings.setActiveChildId(selected.id) }
                combine(
                    getQuota(selected.id),
                    getCollectionsForAge(selected.ageBand),
                ) { quota, collections ->
                    DashboardUiState.Content(
                        children = children,
                        selected = selected,
                        quota = quota,
                        collections = collections.map {
                            CollectionToggle(it, allowed = it.id in selected.allowedCollectionIds && !it.isPremium)
                        },
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DashboardUiState.Loading)

    private val effectChannel = Channel<DashboardEffect>(Channel.BUFFERED)
    val effects: Flow<DashboardEffect> = effectChannel.receiveAsFlow()

    fun onEvent(event: DashboardEvent) {
        val content = state.value as? DashboardUiState.Content
        when (event) {
            is DashboardEvent.SelectChild -> viewModelScope.launch { settings.setActiveChildId(event.childId) }
            DashboardEvent.AddChildClicked -> effectChannel.trySend(DashboardEffect.OpenAddChild)
            is DashboardEvent.LimitChanged -> content?.let { c ->
                viewModelScope.launch { updateChild.setDailyLimit(c.selected.id, event.limit) }
            }
            is DashboardEvent.CollectionToggled -> content?.let { c ->
                viewModelScope.launch { updateChild.setCollectionAllowed(c.selected.id, event.collectionId, event.allowed) }
            }
            is DashboardEvent.GrantExtra -> content?.let { c ->
                viewModelScope.launch { grantExtras(c.selected.id, event.videos) }
            }
            DashboardEvent.ActivityClicked -> content?.let { effectChannel.trySend(DashboardEffect.OpenActivity(it.selected.id)) }
            DashboardEvent.SubscriptionClicked -> effectChannel.trySend(DashboardEffect.OpenSubscription)
            DashboardEvent.LockClicked -> effectChannel.trySend(DashboardEffect.Lock)
        }
    }
}
