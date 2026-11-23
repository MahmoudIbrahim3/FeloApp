package com.mahmoud.clipkid.feature.parent.about

import androidx.lifecycle.ViewModel
import com.mahmoud.clipkid.core.model.LegalLinks
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

data class AboutUiState(val appVersion: String, val supportEmail: String)

sealed interface AboutEvent {
    data object BackClicked : AboutEvent
    data object PrivacyClicked : AboutEvent
    data object TermsClicked : AboutEvent
    data object YouTubeTermsClicked : AboutEvent
    data object GooglePrivacyClicked : AboutEvent
    data object ContactClicked : AboutEvent
}

sealed interface AboutEffect {
    data object Back : AboutEffect

    /** A web page or a `mailto:` link. Safe to open directly: this screen is behind the PIN. */
    data class OpenUrl(val url: String) : AboutEffect
}

/**
 * "عن التطبيق والخصوصية" in parent mode (compliance A14 / C6): our privacy policy and terms,
 * YouTube's Terms of Service, Google's Privacy Policy, and how to reach us.
 */
class AboutViewModel(
    appVersion: String,
    private val links: LegalLinks = LegalLinks.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(AboutUiState(appVersion = appVersion, supportEmail = links.supportEmail))
    val state: StateFlow<AboutUiState> = _state.asStateFlow()

    private val effectChannel = Channel<AboutEffect>(Channel.BUFFERED)
    val effects: Flow<AboutEffect> = effectChannel.receiveAsFlow()

    fun onEvent(event: AboutEvent) {
        val effect = when (event) {
            AboutEvent.BackClicked -> AboutEffect.Back
            AboutEvent.PrivacyClicked -> AboutEffect.OpenUrl(links.privacy)
            AboutEvent.TermsClicked -> AboutEffect.OpenUrl(links.terms)
            AboutEvent.YouTubeTermsClicked -> AboutEffect.OpenUrl(links.youtubeTerms)
            AboutEvent.GooglePrivacyClicked -> AboutEffect.OpenUrl(links.googlePrivacy)
            AboutEvent.ContactClicked -> AboutEffect.OpenUrl("mailto:${links.supportEmail}")
        }
        effectChannel.trySend(effect)
    }
}
