package com.android.systemui.statusbar.phone

// Test-only JVM type with the exact nested name required by the ROM403 privacy contract.
// It is not packaged into production or used as a fallback SystemUI implementation.
class MiuiHomePrivacyController {
    enum class PrivacyState {
        HIDDEN, VISIBLE,
    }
}
