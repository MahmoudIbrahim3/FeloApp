package com.mahmoud.clipkid.core.model

/**
 * Public pages the app links to: our terms and privacy policy (consent step, paywall, parent-mode
 * About screen; App Store 3.1.2 and Google Play want both next to the purchase button), YouTube's
 * Terms of Service and Google's Privacy Policy (YouTube API Services Terms III.A.2), and the
 * support email shown on the About screen.
 *
 * TODO(Mahmoud): replace [Default]'s terms, privacy and supportEmail with the real ones before
 * release (D7); the same URLs go into App Store Connect (App Information) and Play Console (Store listing).
 */
data class LegalLinks(
    val terms: String,
    val privacy: String,
    val supportEmail: String,
    val youtubeTerms: String = YOUTUBE_TERMS,
    val googlePrivacy: String = GOOGLE_PRIVACY,
) {
    companion object {
        const val YOUTUBE_TERMS: String = "https://www.youtube.com/t/terms"
        const val GOOGLE_PRIVACY: String = "https://policies.google.com/privacy"

        val Default: LegalLinks = LegalLinks(
            terms = "https://clipkid.app/terms",
            privacy = "https://clipkid.app/privacy",
            supportEmail = "support@clipkid.app",
        )
    }
}
