package com.chaners.guiyuan.system

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class RuntimeEnv(
    val manufacturer: String,
    val deviceName: String,
    val model: String,
    val codename: String,
    val androidVersion: String,
    val sdk: Int,
    val osVersion: String,
    val systemUiVersionName: String,
    val systemUiVersionCode: Long?,
) {
    val modelAndCodename: String
        get() =
            if (codename.isBlank() || codename.equals(model, ignoreCase = true)) {
                model
            } else {
                "$model ($codename)"
            }

    val androidDisplay: String
        get() = "Android $androidVersion (API $sdk)"

    val systemUiDisplay: String
        get() =
            if (systemUiVersionCode != null) {
                "$systemUiVersionName ($systemUiVersionCode)"
            } else {
                systemUiVersionName
            }

    companion object {
        private const val SystemUiPackageName = "com.android.systemui"
        private val SoftwareRevisionPattern = Regex("^[A-Za-z][0-9]{1,3}$")

        fun basic(): RuntimeEnv =
            RuntimeEnv(
                manufacturer = Build.MANUFACTURER.orEmpty(),
                deviceName = Build.MODEL.orEmpty(),
                model = Build.MODEL.orEmpty(),
                codename = Build.DEVICE.orEmpty(),
                androidVersion = Build.VERSION.RELEASE.orEmpty(),
                sdk = Build.VERSION.SDK_INT,
                osVersion = Build.VERSION.INCREMENTAL.orEmpty(),
                systemUiVersionName = "—",
                systemUiVersionCode = null,
            )

        suspend fun resolve(context: Context): RuntimeEnv =
            withContext(Dispatchers.IO) {
                val marketName =
                    readSystemProperty("ro.product.marketname")
                        .ifBlank { Build.MODEL.orEmpty() }
                val baseOsVersion =
                    readSystemProperty("ro.mi.os.version.incremental")
                        .ifBlank { Build.VERSION.INCREMENTAL.orEmpty() }
                val primaryRevision = readSystemProperty("persist.sys.xms.version")
                val secondaryRevision = readSystemProperty("ro.mi.xms.version.incremental")
                val osVersion =
                    composeOsVersion(
                        baseVersion = baseOsVersion,
                        primaryRevision = primaryRevision,
                        secondaryRevision = secondaryRevision,
                    )

                val systemUiPackage =
                    runCatching {
                        context.packageManager.getPackageInfo(
                            SystemUiPackageName,
                            PackageManager.PackageInfoFlags.of(0),
                        )
                    }.getOrNull()

                RuntimeEnv(
                    manufacturer = Build.MANUFACTURER.orEmpty(),
                    deviceName = marketName,
                    model = Build.MODEL.orEmpty(),
                    codename = Build.DEVICE.orEmpty(),
                    androidVersion = Build.VERSION.RELEASE.orEmpty(),
                    sdk = Build.VERSION.SDK_INT,
                    osVersion = osVersion.ifBlank { Build.VERSION.INCREMENTAL.orEmpty() },
                    systemUiVersionName = systemUiPackage?.versionName.orEmpty().ifBlank { "—" },
                    systemUiVersionCode = systemUiPackage?.longVersionCode,
                )
            }

        internal fun composeOsVersion(
            baseVersion: String,
            primaryRevision: String,
            secondaryRevision: String,
        ): String {
            val normalizedBase =
                baseVersion
                    .trim()
                    .removePrefix("OS")
                    .trim()
            val revision =
                preferredRevision(primaryRevision, secondaryRevision)
                    ?: return normalizedBase

            if (
                normalizedBase.endsWith(".$revision", ignoreCase = true) ||
                normalizedBase.endsWith(revision, ignoreCase = true)
            ) {
                return normalizedBase
            }

            val betaSuffix = " Beta"
            return if (normalizedBase.endsWith(betaSuffix, ignoreCase = true)) {
                normalizedBase.dropLast(betaSuffix.length).trimEnd() +
                    "." + revision + normalizedBase.takeLast(betaSuffix.length)
            } else {
                "$normalizedBase.$revision"
            }
        }

        private fun preferredRevision(
            primary: String,
            secondary: String,
        ): String? {
            val first = primary.trim().takeIf(SoftwareRevisionPattern::matches)
            val second = secondary.trim().takeIf(SoftwareRevisionPattern::matches)
            if (first == null) return second
            if (second == null) return first

            val firstPrefix = first.first().lowercaseChar()
            val secondPrefix = second.first().lowercaseChar()
            if (firstPrefix != secondPrefix) {
                return if (firstPrefix > secondPrefix) first else second
            }

            val firstNumber = first.drop(1).toIntOrNull() ?: return second
            val secondNumber = second.drop(1).toIntOrNull() ?: return first
            return if (firstNumber >= secondNumber) first else second
        }

        private fun readSystemProperty(key: String): String {
            val reflected =
                runCatching {
                    val clazz = Class.forName("android.os.SystemProperties")
                    val method = clazz.getMethod("get", String::class.java, String::class.java)
                    method.invoke(null, key, "") as? String
                }.getOrNull().orEmpty()
            if (reflected.isNotBlank()) {
                return reflected.trim()
            }

            return runCatching {
                val process = ProcessBuilder("/system/bin/getprop", key)
                    .redirectErrorStream(true)
                    .start()
                process.inputStream.bufferedReader().use { it.readLine().orEmpty().trim() }
            }.getOrDefault("")
        }
    }
}
