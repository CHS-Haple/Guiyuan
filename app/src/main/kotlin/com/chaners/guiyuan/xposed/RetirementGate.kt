package com.chaners.guiyuan.xposed

// Keeps a failed release reachable and forbids a second owner or reentrant release.
internal class RetirementGate<T : Any> {
    var pending: T? = null
        private set
    private var releasing = false

    fun release(owner: T, action: () -> Int): Int {
        check(!releasing) { "release-reentered" }
        check(pending == null || pending === owner) { "release-incomplete" }

        pending = owner
        releasing = true
        try {
            val restored = action()
            if (pending === owner) pending = null
            return restored
        } finally {
            releasing = false
        }
    }
}
