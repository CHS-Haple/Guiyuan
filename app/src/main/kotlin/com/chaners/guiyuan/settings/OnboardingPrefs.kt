package com.chaners.guiyuan.settings

import android.content.Context

internal data class AgreementReadingRequirements(
    val terms: Boolean,
    val privacy: Boolean,
) {
    fun canAcknowledge(termsViewed: Boolean, privacyViewed: Boolean): Boolean =
        (!terms || termsViewed) && (!privacy || privacyViewed)
}

internal object OnboardingPrefs {
    private const val GUIDE_PREFS = "guiyuan_first_run"
    private const val GUIDE_COMPLETED = "completed"
    private const val GUIDE_STARTED = "started"
    private const val PRIVACY_ACK_KEY = "privacy_ack_version"
    private const val TERMS_ACK_KEY = "terms_ack_version"
    // Increment only when a substantive agreement revision requires fresh acknowledgment.
    private const val PRIVACY_ACK_VERSION = 4
    private const val TERMS_ACK_VERSION = 3

    internal fun hasCurrentAgreementAcknowledgment(privacy: Int, terms: Int): Boolean =
        privacy >= PRIVACY_ACK_VERSION && terms >= TERMS_ACK_VERSION

    internal fun readingRequirements(privacyVersion: Int, termsVersion: Int) =
        AgreementReadingRequirements(
            terms = termsVersion < TERMS_ACK_VERSION,
            privacy = privacyVersion < PRIVACY_ACK_VERSION,
        )

    fun readingRequirements(ctx: Context): AgreementReadingRequirements {
        val prefs = ctx.getSharedPreferences(GUIDE_PREFS, Context.MODE_PRIVATE)
        return readingRequirements(
            prefs.getInt(PRIVACY_ACK_KEY, 0),
            prefs.getInt(TERMS_ACK_KEY, 0),
        )
    }

    fun needsAgreementReview(ctx: Context): Boolean {
        val prefs = ctx.getSharedPreferences(GUIDE_PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(GUIDE_COMPLETED, false) &&
            !hasCurrentAgreementAcknowledgment(
                prefs.getInt(PRIVACY_ACK_KEY, 0),
                prefs.getInt(TERMS_ACK_KEY, 0),
            )
    }

    fun needsOnboarding(ctx: Context): Boolean {
        val prefs = ctx.getSharedPreferences(GUIDE_PREFS, Context.MODE_PRIVATE)
        if (prefs.contains(GUIDE_COMPLETED)) {
            return !prefs.getBoolean(GUIDE_COMPLETED, false) || needsAgreementReview(ctx)
        }
        // A guide already started must stay pending across app updates.
        prefs.edit().putBoolean(GUIDE_STARTED, true).apply()
        return true
    }

    fun markExistingInstall(ctx: Context) {
        val prefs = ctx.getSharedPreferences(GUIDE_PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(GUIDE_COMPLETED) && !prefs.contains(GUIDE_STARTED)) {
            prefs.edit().putBoolean(GUIDE_COMPLETED, true).apply()
        }
    }

    fun completeOnboarding(ctx: Context) {
        ctx.getSharedPreferences(GUIDE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(PRIVACY_ACK_KEY, PRIVACY_ACK_VERSION)
            .putInt(TERMS_ACK_KEY, TERMS_ACK_VERSION)
            .putBoolean(GUIDE_COMPLETED, true)
            .apply()
    }
}
