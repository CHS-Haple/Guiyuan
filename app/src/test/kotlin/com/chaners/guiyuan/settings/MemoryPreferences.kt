package com.chaners.guiyuan.settings

import android.content.SharedPreferences

internal class MemoryPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any>()
    var writeCount = 0
        private set
    var failNextCommit = false

    override fun getAll(): MutableMap<String, *> = values.toMutableMap()
    override fun contains(key: String?): Boolean = values.containsKey(key)
    override fun getString(key: String?, defValue: String?): String? =
        values[key] as? String ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(
        key: String?,
        defValues: MutableSet<String>?,
    ): MutableSet<String>? = (values[key] as? Set<String>)?.toMutableSet() ?: defValues

    override fun getInt(key: String?, defValue: Int): Int =
        values[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long =
        values[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float =
        values[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        values[key] as? Boolean ?: defValue

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    override fun edit(): SharedPreferences.Editor = Editor()

    private inner class Editor : SharedPreferences.Editor {
        private val changes = linkedMapOf<String, Any?>()
        private var clearFirst = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor =
            put(key, value)
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor =
            put(key, values?.toSet())
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor =
            put(key, value)
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor =
            put(key, value)
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor =
            put(key, value)
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor =
            put(key, value)
        override fun remove(key: String?): SharedPreferences.Editor = put(key, null)

        override fun clear(): SharedPreferences.Editor {
            clearFirst = true
            return this
        }

        private fun put(key: String?, value: Any?): SharedPreferences.Editor {
            if (key != null) changes[key] = value
            return this
        }

        override fun commit(): Boolean {
            writeCount++
            if (clearFirst) values.clear()
            changes.forEach { (key, value) ->
                if (value == null) values.remove(key) else values[key] = value
            }
            // SharedPreferences may update memory even when durable commit fails.
            val durable = !failNextCommit
            failNextCommit = false
            return durable
        }

        override fun apply() {
            commit()
        }
    }
}
