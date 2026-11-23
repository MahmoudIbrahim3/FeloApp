package com.mahmoud.feelo.feature.parent.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahmoud.feelo.core.domain.repository.ProfileRepository
import com.mahmoud.feelo.core.domain.repository.SettingsRepository
import com.mahmoud.feelo.core.domain.usecase.ConnectBillingUseCase
import com.mahmoud.feelo.core.domain.usecase.GetCollectionsForAgeUseCase
import com.mahmoud.feelo.core.domain.usecase.GetDailyQuotaUseCase
import com.mahmoud.feelo.core.domain.usecase.GrantExtraVideosUseCase
import com.mahmoud.feelo.core.domain.usecase.ObserveAccessUseCase
import com.mahmoud.feelo.core.domain.usecase.UpdateChildSettingsUseCase
import com.mahmoud.feelo.core.model.ChildProfile
import com.mahmoud.feelo.core.model.Collection
import com.mahmoud.feelo.core.model.DailyLimit
import com.mahmoud.feelo.core.model.DailyQuota
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

/** One collection switch. Every collection is free. */
data class CollectionToggle(val collection: Collection, val allowed: Boolean)

/** What the "+" after the child chips does. */
enum class AddChildAction {
    /** Opens the add-child steps. */
    Add,

    /** Free plan with its one child: opens the paywall. */
    Subscribe,

    /** Full plan at its limit (4): the "+" is hidden. */
    Hidden,
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
        /** Free plan only: profiles beyond the first, kept but not usable until the subscription is back. */
        val lockedChildIds: Set<String> = emptySet(),
        val addChild: AddChildAction = AddChildAction.Add,
        val isSubscribed: Boolean = false,
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

    /** "عن التطبيق والخصوصية": policies, terms and contact. */
    data object AboutClicked : DashboardEvent

    /** Lock button: back to child mode. */
    data object LockClicked : DashboardEvent
}

sealed interface DashboardEffect {
    data object OpenAddChild : DashboardEffect
    data class OpenActivity(val childId: String) : DashboardEffect
    data object OpenSubscription : DashboardEffect
    data object OpenAbout : DashboardEffect
    data object Lock : DashboardEffect
}

/**
 * Parent dashboard (Parent.png). Every change is saved at once, so child Home follows it.
 *
 * Free vs full: without the subscription, extra children show a lock and open the paywall (this
 * screen is already behind the PIN). Every collection is free. Nothing is deleted on expiry.
 */
class DashboardViewModel(
    private val profiles: ProfileRepository,
    private val settings: SettingsRepository,
    private val getQuota: GetDailyQuotaUseCase,
    private val getCollectionsForAge: GetCollectionsForAgeUseCase,
    private val updateChild: UpdateChildSettingsUseCase,
    private val grantExtras: GrantExtraVideosUseCase,
    private val observeAccess: ObserveAccessUseCase,
    connectBilling: ConnectBillingUseCase,
) : ViewModel() {

    init {
        // Parent mode: the latest subscription status (renewal, cancellation), if the parent agreed.
        viewModelScope.launch { connectBilling.forParent() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<DashboardUiState> = combine(
        profiles.observeProfiles(),
        settings.activeChildId,
        observeAccess(),
    ) { all, activeId, access -> Triple(all, activeId, access) }
        .flatMapLatest { (all, activeId, access) ->
            val children = all
            val usable = children.filterNot { access.isChildLocked(it.id) }
            val selected = usable.firstOrNull { it.id == activeId } ?: usable.firstOrNull()
            if (selected == null) {
                flowOf(DashboardUiState.NoChildren)
            } else {
                // No, a deleted or a locked active child: the first usable one becomes active.
                if (selected.id != activeId) viewModelScope.launch { settings.setActiveChildId(selected.id) }
                combine(
                    getQuota(selected.id),
                    getCollectionsForAge(selected.ageBand),
                ) { quota, collections ->
                    DashboardUiState.Content(
                        children = children,
                        selected = selected,
                        quota = quota,
                        collections = collections.map { CollectionToggle(it, allowed = it.id in selected.allowedCollectionIds) },
                        lockedChildIds = children.mapNotNullTo(HashSet()) { c -> c.id.takeIf(access::isChildLocked) },
                        addChild = when {
                            access.canAddChild -> AddChildAction.Add
                            access.addChildNeedsSubscription -> AddChildAction.Subscribe
                            else -> AddChildAction.Hidden
                        },
                        isSubscribed = access.isFull,
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
            is DashboardEvent.SelectChild ->
                if (content != null && event.childId in content.lockedChildIds) {
                    effectChannel.trySend(DashboardEffect.OpenSubscription)
                } else {
                    viewModelScope.launch { settings.setActiveChildId(event.childId) }
                }
            DashboardEvent.AddChildClicked -> when (content?.addChild ?: AddChildAction.Add) {
                AddChildAction.Add -> effectChannel.trySend(DashboardEffect.OpenAddChild)
                AddChildAction.Subscribe -> effectChannel.trySend(DashboardEffect.OpenSubscription)
                AddChildAction.Hidden -> Unit
            }
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
            DashboardEvent.AboutClicked -> effectChannel.trySend(DashboardEffect.OpenAbout)
            DashboardEvent.LockClicked -> effectChannel.trySend(DashboardEffect.Lock)
        }
    }
}
