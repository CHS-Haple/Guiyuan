package com.chaners.guiyuan.ui.screens

import com.chaners.guiyuan.R
import com.chaners.guiyuan.system.LogLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DiagnosticsLabelsTest {
    // Emitted event names are independent of the presentation map.
    private val events = listOf(
        "aod.authority",
        "aod.batteryHandoff",
        "aod.ccAppearance",
        "aod.geometry",
        "aod.homePrearm",
        "aod.homeTransientKeyguard",
        "aod.nativeSleepLinkage",
        "aod.state",
        "aod.target",
        "aod.targetWindow",
        "aod.visibleLayers",
        "aod.visualBoundary",
        "aod.visualHandoff",
        "aod.windowWitness",
        "compatibility.probe",
        "compatibility.revalidated",
        "diagnostics.bind",
        "diagnostics.level",
        "diagnostics.snapshot",
        "featureSettings.changed",
        "featureSettings.dispatch",
        "glyph.seed",
        "hook.install",
        "hook.replace",
        "host.capture",
        "host.resolve",
        "host.restore",
        "hotReload.cleanup",
        "hotReload.complete",
        "hotReload.generationHandoff",
        "hotReload.migration",
        "hotReload.prepare",
        "hotReload.restore",
        "keyguard.hostProbe",
        "mobile.recovery",
        "module.loaded",
        "module.reloaded",
        "pipeline.latency",
        "presentation.cutover",
        "presentation.failNative",
        "presentation.lease",
        "presentation.readiness",
        "presentation.resolve",
        "projection.attach",
        "projection.hiddenLayers",
        "projection.prearm",
        "projection.prepare",
        "projection.restore",
        "projection.visibleCycle",
        "projection.yield",
        "renderer.attach",
        "runtime.attach",
        "runtime.callback",
        "runtime.teardown",
        "runtimePreferences.bind",
        "sandbox.lease",
        "scene.defer",
        "scene.eligibility",
        "scene.stableFamily",
        "session.attach",
        "slot.probe",
        "source.attach",
        "source.install",
        "source.install.branch",
        "target.probe",
        "transition.geometry",
        "visualSettings.changed",
        "visualSettings.dispatch",
    )

    @Test fun knownEventsHaveTitles() {
        events.forEach { name ->
            assertNotEquals(name, R.string.diagnostics_log_event_generic, titleRes(name))
        }
        assertEquals(R.string.diagnostics_log_event_generic, titleRes("future.event"))
    }

    @Test fun statesHandleSceneEnumsAndUnknownValues() {
        assertEquals(R.string.diagnostics_log_state_aod_stable, stateRes("AOD_STABLE"))
        assertEquals(R.string.diagnostics_log_state_unknown, stateRes("UNKNOWN"))
        assertEquals(R.string.diagnostics_log_state_preparing, stateRes("preparing"))
        assertEquals(R.string.diagnostics_log_state_other, stateRes("future"))
    }

    @Test fun dynamicFieldsFallBackToLocalizedHints() {
        assertEquals(R.string.diagnostics_log_source_other, sourceRes("future-source"))
        assertEquals(R.string.diagnostics_log_reason_details, reasonRes("ExceptionClassName"))
        assertEquals(R.string.diagnostics_log_level_info, levelRes(LogLevel.Info))
    }
}
