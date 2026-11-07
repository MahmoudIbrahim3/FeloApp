package com.mahmoud.clipkid.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahmoud.clipkid.core.designsystem.component.AvatarColors
import com.mahmoud.clipkid.core.domain.parent.PIN_LENGTH
import com.mahmoud.clipkid.core.domain.usecase.AddChildUseCase
import com.mahmoud.clipkid.core.domain.usecase.CompleteOnboardingUseCase
import com.mahmoud.clipkid.core.domain.usecase.DailyLimitOptions
import com.mahmoud.clipkid.core.domain.usecase.GetCollectionsForAgeUseCase
import com.mahmoud.clipkid.core.domain.usecase.NICKNAME_MAX_LENGTH
import com.mahmoud.clipkid.core.domain.usecase.NewChild
import com.mahmoud.clipkid.core.domain.usecase.isValidNickname
import com.mahmoud.clipkid.core.model.AgeBand
import com.mahmoud.clipkid.core.model.Collection
import com.mahmoud.clipkid.core.model.DailyLimit
import com.mahmoud.clipkid.core.model.LegalLinks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** First launch (welcome, PIN, consent, child, collections) or adding one more child from the dashboard. */
enum class OnboardingMode { FirstRun, AddChild }

/** The PIN step: first entry, then the same PIN again to confirm. */
data class PinStep(
    val entered: String = "",
    /** The first entry, once confirmed with "التالي"; null while still on the first entry. */
    val first: String? = null,
    /** The confirmation did not match; the step started over. */
    val mismatch: Boolean = false,
) {
    val confirming: Boolean get() = first != null
    val isComplete: Boolean get() = entered.length == PIN_LENGTH
}

data class CollectionOption(val collection: Collection, val selected: Boolean)

data class OnboardingUiState(
    val mode: OnboardingMode,
    val pin: PinStep = PinStep(),
    val nickname: String = "",
    val avatarColor: Long = AvatarColors.default,
    val ageBand: AgeBand? = null,
    val collections: List<CollectionOption> = emptyList(),
    val limit: DailyLimit = DailyLimitOptions.defaultLimit,
    /** First run: the parent agreed on the consent step. Saved with everything else at Finish. */
    val consentAgreed: Boolean = false,
    val saving: Boolean = false,
    /** Saving failed; the parent can try again. */
    val saveFailed: Boolean = false,
) {
    val canContinueChild: Boolean get() = isValidNickname(nickname) && ageBand != null
    val canFinish: Boolean
        get() = canContinueChild && collections.any { it.selected } && !saving &&
            (mode == OnboardingMode.AddChild || consentAgreed)

    /** The avatar shows the first letter of the nickname. */
    val initial: String get() = nickname.trim().take(1)

    /** Step numbers shown as "خطوة x من y": the PIN and consent steps only exist on first run. */
    val totalSteps: Int get() = if (mode == OnboardingMode.FirstRun) 4 else 2
    val pinStep: Int get() = 1
    val consentStep: Int get() = 2
    val childStep: Int get() = if (mode == OnboardingMode.FirstRun) 3 else 1
    val collectionsStep: Int get() = totalSteps
}

sealed interface OnboardingEvent {
    data object StartClicked : OnboardingEvent
    data class PinDigit(val digit: Int) : OnboardingEvent
    data object PinBackspace : OnboardingEvent
    data object PinNext : OnboardingEvent

    /** Back on the PIN step: while confirming, it returns to the first entry. */
    data object PinBackClicked : OnboardingEvent

    /** Consent step: "موافق، كمّل". */
    data object ConsentAgreed : OnboardingEvent
    data object PrivacyClicked : OnboardingEvent
    data object YouTubeTermsClicked : OnboardingEvent
    data class NicknameChanged(val value: String) : OnboardingEvent
    data class AvatarPicked(val argb: Long) : OnboardingEvent
    data class AgePicked(val band: AgeBand) : OnboardingEvent
    data object ChildNext : OnboardingEvent
    data class CollectionToggled(val collectionId: String, val on: Boolean) : OnboardingEvent
    data class LimitChanged(val limit: DailyLimit) : OnboardingEvent
    data object FinishClicked : OnboardingEvent
    data object BackClicked : OnboardingEvent
}

sealed interface OnboardingEffect {
    data object OpenPin : OnboardingEffect
    data object OpenConsent : OnboardingEffect
    data object OpenChild : OnboardingEffect

    /** Our privacy policy or YouTube's terms. Only from the consent step, after the PIN is set. */
    data class OpenUrl(val url: String) : OnboardingEffect
    data object OpenCollections : OnboardingEffect

    /** Saved. First run: open child mode for [childId]. Add child: back to the dashboard. */
    data class Finished(val childId: String) : OnboardingEffect
    data object Back : OnboardingEffect
}

/**
 * Drives all onboarding steps; shared by the step screens (scoped to the onboarding graph).
 * Nothing is saved until [OnboardingEvent.FinishClicked]: the PIN stays in memory until then.
 */
class OnboardingViewModel(
    private val mode: OnboardingMode,
    private val getCollectionsForAge: GetCollectionsForAgeUseCase,
    private val completeOnboarding: CompleteOnboardingUseCase,
    private val addChild: AddChildUseCase,
    private val links: LegalLinks = LegalLinks.Default,
) : ViewModel() {

    private val form = MutableStateFlow(OnboardingUiState(mode = mode))

    /** Explicit choices; null = the default for the age band (everything). */
    private val selection = MutableStateFlow<Set<String>?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val available: Flow<List<Collection>> = form
        .map { it.ageBand }
        .distinctUntilChanged()
        .flatMapLatest { band -> if (band == null) flowOf(emptyList()) else getCollectionsForAge(band) }

    val state: StateFlow<OnboardingUiState> = combine(form, available, selection) { f, collections, chosen ->
        val selectedIds = chosen ?: GetCollectionsForAgeUseCase.defaultSelection(collections)
        f.copy(collections = collections.map { CollectionOption(it, it.id in selectedIds) })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, form.value)

    private val effectChannel = Channel<OnboardingEffect>(Channel.BUFFERED)
    val effects: Flow<OnboardingEffect> = effectChannel.receiveAsFlow()

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            OnboardingEvent.StartClicked -> effectChannel.trySend(OnboardingEffect.OpenPin)
            is OnboardingEvent.PinDigit -> form.update {
                if (it.pin.isComplete) it else it.copy(pin = it.pin.copy(entered = it.pin.entered + event.digit, mismatch = false))
            }
            OnboardingEvent.PinBackspace -> form.update { it.copy(pin = it.pin.copy(entered = it.pin.entered.dropLast(1))) }
            OnboardingEvent.PinNext -> pinNext()
            OnboardingEvent.PinBackClicked ->
                if (form.value.pin.confirming) form.update { it.copy(pin = PinStep()) } else effectChannel.trySend(OnboardingEffect.Back)
            OnboardingEvent.ConsentAgreed -> {
                form.update { it.copy(consentAgreed = true) }
                effectChannel.trySend(OnboardingEffect.OpenChild)
            }
            OnboardingEvent.PrivacyClicked -> effectChannel.trySend(OnboardingEffect.OpenUrl(links.privacy))
            OnboardingEvent.YouTubeTermsClicked -> effectChannel.trySend(OnboardingEffect.OpenUrl(links.youtubeTerms))
            is OnboardingEvent.NicknameChanged -> form.update { it.copy(nickname = event.value.take(NICKNAME_MAX_LENGTH)) }
            is OnboardingEvent.AvatarPicked -> form.update { it.copy(avatarColor = event.argb) }
            is OnboardingEvent.AgePicked -> {
                if (form.value.ageBand != event.band) selection.value = null
                form.update { it.copy(ageBand = event.band) }
            }
            OnboardingEvent.ChildNext ->
                if (state.value.canContinueChild) effectChannel.trySend(OnboardingEffect.OpenCollections)
            is OnboardingEvent.CollectionToggled -> toggle(event.collectionId, event.on)
            is OnboardingEvent.LimitChanged -> form.update { it.copy(limit = event.limit) }
            OnboardingEvent.FinishClicked -> finish()
            OnboardingEvent.BackClicked -> effectChannel.trySend(OnboardingEffect.Back)
        }
    }

    private fun pinNext() {
        val pin = form.value.pin
        if (!pin.isComplete) return
        val first = pin.first
        when {
            first == null -> form.update { it.copy(pin = PinStep(first = pin.entered)) }
            // The PIN step only exists on first run; the consent step comes right after it.
            first == pin.entered -> effectChannel.trySend(OnboardingEffect.OpenConsent)
            else -> form.update { it.copy(pin = PinStep(mismatch = true)) }
        }
    }

    private fun toggle(collectionId: String, on: Boolean) {
        val current = state.value.collections
        if (current.none { it.collection.id == collectionId }) return
        val ids = current.filter { it.selected }.map { it.collection.id }.toSet()
        selection.value = if (on) ids + collectionId else ids - collectionId
    }

    private fun finish() {
        val s = state.value
        if (!s.canFinish) return
        val band = s.ageBand ?: return
        val child = NewChild(
            nickname = s.nickname,
            avatarColor = s.avatarColor,
            ageBand = band,
            allowedCollectionIds = s.collections.filter { it.selected }.map { it.collection.id }.toSet(),
            dailyLimit = s.limit,
        )
        form.update { it.copy(saving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                val id = when (mode) {
                    OnboardingMode.FirstRun -> completeOnboarding(pin = s.pin.first ?: s.pin.entered, child = child)
                    OnboardingMode.AddChild -> addChild(child)
                }
                // The PIN is not needed any more; do not keep it in memory.
                form.update { it.copy(pin = PinStep(), saving = false) }
                effectChannel.send(OnboardingEffect.Finished(id))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                form.update { it.copy(saving = false, saveFailed = true) }
            }
        }
    }
}
