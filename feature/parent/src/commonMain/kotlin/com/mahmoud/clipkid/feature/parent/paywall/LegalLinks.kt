package com.mahmoud.clipkid.feature.parent.paywall

/**
 * Terms of use and privacy policy, linked from the paywall (App Store guideline 3.1.2 and Google
 * Play subscription policy require both next to the purchase button).
 *
 * TODO(Mahmoud): replace with the real pages before release; the same URLs go into App Store
 * Connect (App Information) and Play Console (Store listing → Privacy policy).
 */
data class LegalLinks(
    val terms: String,
    val privacy: String,
) {
    companion object {
        val Default: LegalLinks = LegalLinks(
            terms = "https://clipkid.app/terms",
            privacy = "https://clipkid.app/privacy",
        )
    }
}
