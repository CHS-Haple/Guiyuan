package com.chaners.guiyuan.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Process
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

// A read-only report from verified SystemUI source, never a control surface.
internal object NativeFontAxis {
    const val ACTION = "com.chaners.guiyuan.NATIVE_FONT_AXIS"
    const val RECEIVER = "com.chaners.guiyuan.settings.NativeFontAxisReceiver"
    const val MIN = "min"
    const val DEFAULT = "default"
    const val MAX = "max"
    private const val PREFS = "native_font_axis"

    fun checked(axis: FontWeightAxis?): FontWeightAxis? =
        axis?.takeIf {
            it.min.isFinite() && it.default.isFinite() && it.max.isFinite() &&
                it.min >= 1f && it.min <= it.default && it.default <= it.max &&
                it.max <= 1000f && it.min < it.max
        }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun read(prefs: SharedPreferences): FontWeightAxis? =
        runCatching {
            checked(
                FontWeightAxis(
                    min = prefs.getFloat(MIN, Float.NaN),
                    default = prefs.getFloat(DEFAULT, Float.NaN),
                    max = prefs.getFloat(MAX, Float.NaN),
                ),
            )
        }.getOrNull()

    fun observe(context: Context): Flow<FontWeightAxis?> = callbackFlow {
        val prefs = prefs(context.applicationContext)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == MIN || key == DEFAULT || key == MAX) trySend(read(prefs))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(read(prefs))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun save(context: Context, axis: FontWeightAxis?) {
        val editor = prefs(context).edit()
        if (axis == null) {
            editor.remove(MIN).remove(DEFAULT).remove(MAX)
        } else {
            editor.putFloat(MIN, axis.min)
                .putFloat(DEFAULT, axis.default)
                .putFloat(MAX, axis.max)
        }
        editor.apply()
    }
}

class NativeFontAxisReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT < 34 ||
            sentFromUid != Process.SYSTEM_UID ||
            sentFromPackage != "com.android.systemui" ||
            intent.action != NativeFontAxis.ACTION
        ) return

        val axis =
            if (intent.hasExtra(NativeFontAxis.MIN) &&
                intent.hasExtra(NativeFontAxis.DEFAULT) &&
                intent.hasExtra(NativeFontAxis.MAX)
            ) {
                NativeFontAxis.checked(
                    FontWeightAxis(
                        intent.getFloatExtra(NativeFontAxis.MIN, Float.NaN),
                        intent.getFloatExtra(NativeFontAxis.DEFAULT, Float.NaN),
                        intent.getFloatExtra(NativeFontAxis.MAX, Float.NaN),
                    ),
                )
            } else null
        NativeFontAxis.save(context, axis)
    }
}
