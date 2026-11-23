package com.mahmoud.clipkid.feature.parent

import app.cash.turbine.test
import com.mahmoud.clipkid.core.domain.testing.runViewModelTest
import com.mahmoud.clipkid.core.model.LegalLinks
import com.mahmoud.clipkid.feature.parent.about.AboutEffect
import com.mahmoud.clipkid.feature.parent.about.AboutEvent
import com.mahmoud.clipkid.feature.parent.about.AboutUiState
import com.mahmoud.clipkid.feature.parent.about.AboutViewModel
import kotlin.test.Test
import kotlin.test.assertEquals

class AboutViewModelTest {

    private val links = LegalLinks(terms = "https://example.com/terms", privacy = "https://example.com/privacy", supportEmail = "help@example.com")

    @Test
    fun stateCarriesTheVersionAndTheEmail() = runViewModelTest {
        val vm = AboutViewModel("1.2", links)
        assertEquals(AboutUiState(appVersion = "1.2", supportEmail = "help@example.com"), vm.state.value)
    }

    @Test
    fun eachLinkOpensItsPage() = runViewModelTest {
        val vm = AboutViewModel("1.0", links)
        vm.effects.test {
            vm.onEvent(AboutEvent.PrivacyClicked)
            assertEquals(AboutEffect.OpenUrl("https://example.com/privacy"), awaitItem())
            vm.onEvent(AboutEvent.TermsClicked)
            assertEquals(AboutEffect.OpenUrl("https://example.com/terms"), awaitItem())
            vm.onEvent(AboutEvent.YouTubeTermsClicked)
            assertEquals(AboutEffect.OpenUrl(LegalLinks.YOUTUBE_TERMS), awaitItem())
            vm.onEvent(AboutEvent.GooglePrivacyClicked)
            assertEquals(AboutEffect.OpenUrl(LegalLinks.GOOGLE_PRIVACY), awaitItem())
        }
    }

    @Test
    fun contactOpensAMailLink() = runViewModelTest {
        val vm = AboutViewModel("1.0", links)
        vm.effects.test {
            vm.onEvent(AboutEvent.ContactClicked)
            assertEquals(AboutEffect.OpenUrl("mailto:help@example.com"), awaitItem())
        }
    }

    @Test
    fun backGoesBack() = runViewModelTest {
        val vm = AboutViewModel("1.0", links)
        vm.effects.test {
            vm.onEvent(AboutEvent.BackClicked)
            assertEquals(AboutEffect.Back, awaitItem())
        }
    }
}
