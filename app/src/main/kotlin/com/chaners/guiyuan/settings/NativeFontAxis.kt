package com.chaners.guiyuan.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.lang.reflect.InvocationTargetException
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
    const val BAT_MIN = "battery_min"
    const val BAT_DEFAULT = "battery_default"
    const val BAT_MAX = "battery_max"
    const val REQUEST_KEY = "native_font_axis_request"
    private const val PREFS = "native_font_axis"
    private const val BOOT = "boot_count"
    private const val REPORTED = "reported"

    internal sealed interface State {
        data object Pending : State
        data object Unavailable : State
        data class Variable(val axis: FontWeightAxis) : State
    }

    internal fun state(reported: Boolean, axis: FontWeightAxis?): State =
        if (!reported) State.Pending
        else checked(axis)?.let(State::Variable) ?: State.Unavailable

    fun checked(axis: FontWeightAxis?): FontWeightAxis? =
        axis?.takeIf {
            it.min.isFinite() && it.default.isFinite() && it.max.isFinite() &&
                it.min >= 1f && it.min <= it.default && it.default <= it.max &&
                it.max <= 1000f && it.min < it.max
        }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    internal fun rethrowFatal(error: Throwable) {
        val cause = (error as? InvocationTargetException)?.targetException ?: error
        if (cause is VirtualMachineError || cause is ThreadDeath) throw cause
    }

    private fun bootCount(context: Context): Int =
        runCatching {
            Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
        }.onFailure(::rethrowFatal).getOrDefault(-1)

    private fun read(
        context: Context,
        prefs: SharedPreferences,
        min: String,
        default: String,
        max: String,
    ): State = runCatching {
        val boot = bootCount(context)
        if (boot < 0 || prefs.getInt(BOOT, -1) != boot) return@runCatching State.Pending
        state(
            reported = prefs.getBoolean(REPORTED, false),
            axis = FontWeightAxis(
                min = prefs.getFloat(min, Float.NaN),
                default = prefs.getFloat(default, Float.NaN),
                max = prefs.getFloat(max, Float.NaN),
            ),
        )
    }.onFailure(::rethrowFatal).getOrDefault(State.Pending)

    fun observe(context: Context): Flow<State> =
        observe(context, MIN, DEFAULT, MAX)

    fun observeBattery(context: Context): Flow<State> =
        observe(context, BAT_MIN, BAT_DEFAULT, BAT_MAX)

    private fun observe(
        context: Context,
        min: String,
        default: String,
        max: String,
    ): Flow<State> = callbackFlow {
        val app = context.applicationContext
        val prefs = prefs(app)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == min || key == default || key == max || key == BOOT || key == REPORTED) {
                trySend(read(app, prefs, min, default, max))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(read(app, prefs, min, default, max))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun save(context: Context, mobile: FontWeightAxis?, battery: FontWeightAxis?) {
        val boot = bootCount(context)
        val editor = prefs(context).edit()
        fun write(axis: FontWeightAxis?, min: String, default: String, max: String) {
            if (axis == null || boot < 0) {
                editor.remove(min).remove(default).remove(max)
            } else {
                editor.putFloat(min, axis.min)
                    .putFloat(default, axis.default)
                    .putFloat(max, axis.max)
            }
        }
        write(mobile, MIN, DEFAULT, MAX)
        write(battery, BAT_MIN, BAT_DEFAULT, BAT_MAX)
        if (boot < 0) editor.remove(BOOT).remove(REPORTED)
        else editor.putInt(BOOT, boot).putBoolean(REPORTED, true)
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

        fun axis(min: String, default: String, max: String): FontWeightAxis? =
            if (intent.hasExtra(min) && intent.hasExtra(default) && intent.hasExtra(max)) {
                NativeFontAxis.checked(
                    FontWeightAxis(
                        intent.getFloatExtra(min, Float.NaN),
                        intent.getFloatExtra(default, Float.NaN),
                        intent.getFloatExtra(max, Float.NaN),
                    ),
                )
            } else null
        NativeFontAxis.save(
            context,
            mobile = axis(NativeFontAxis.MIN, NativeFontAxis.DEFAULT, NativeFontAxis.MAX),
            battery = axis(NativeFontAxis.BAT_MIN, NativeFontAxis.BAT_DEFAULT, NativeFontAxis.BAT_MAX),
        )
    }
}
