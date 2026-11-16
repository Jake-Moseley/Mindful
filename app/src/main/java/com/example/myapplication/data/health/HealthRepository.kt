package com.example.myapplication.data.health

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Where Health Connect stands on this phone. */
enum class HealthStatus {
    LOADING,
    NOT_SUPPORTED,      // Health Connect can't run on this phone
    NEEDS_INSTALL,      // Older Android: the Health Connect app needs to be installed or updated
    NEEDS_PERMISSION,   // Health Connect is here, but the user hasn't allowed Mindful to read sleep and water
    READY
}

/** What the home cards and the Weekly Story need from Health Connect. */
data class HealthSnapshot(
    val status: HealthStatus = HealthStatus.LOADING,
    /** Sleep that ended today ("last night"), or null if nothing was recorded. */
    val sleepLastNight: Duration? = null,
    /** Water logged today, in liters. */
    val waterTodayLiters: Double = 0.0,
    /** Hours slept, keyed by the day the user woke up. Covers the last [HealthRepository.HISTORY_DAYS] days. */
    val sleepByDate: Map<LocalDate, Double> = emptyMap()
)

/**
 * Reads sleep and water from Health Connect (Android's shared health data store).
 * Apps like Samsung Health, Fitbit and Google Fit write there; Mindful only reads, and only with permission.
 */
class HealthRepository(private val context: Context) {

    companion object {
        private const val TAG = "HealthRepository"

        /** The Play Store page for Health Connect (used on Android 13 and older). */
        const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

        /** How far back sleep is read, to match the Weekly Story's 30-day lookback. */
        const val HISTORY_DAYS = 30L

        /** Daily goals shown on the cards. */
        const val SLEEP_GOAL_HOURS = 8.0
        const val WATER_GOAL_LITERS = 2.0

        /** The permissions Mindful asks for. Both must also be declared in AndroidManifest.xml. */
        val PERMISSIONS: Set<String> = setOf(
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(HydrationRecord::class)
        )
    }

    fun availability(): HealthStatus = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthStatus.READY
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthStatus.NEEDS_INSTALL
        else -> HealthStatus.NOT_SUPPORTED
    }

    /** Loads everything at once. Never throws: any problem comes back as a status. */
    suspend fun load(zone: ZoneId = ZoneId.systemDefault()): HealthSnapshot {
        val available = availability()
        if (available != HealthStatus.READY) return HealthSnapshot(status = available)

        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            if (!granted.containsAll(PERMISSIONS)) {
                return HealthSnapshot(status = HealthStatus.NEEDS_PERMISSION)
            }

            val now = Instant.now()
            val today = LocalDate.now(zone)
            val startOfToday = today.atStartOfDay(zone).toInstant()

            // Sleep: every session in the last 30 days, added up by the day the user woke up
            val sleepSessions = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        today.minusDays(HISTORY_DAYS).atStartOfDay(zone).toInstant(),
                        now
                    )
                )
            ).records
            val sleepByDate = sleepSessions
                .groupBy { it.endTime.atZone(zone).toLocalDate() }
                .mapValues { (_, sessions) ->
                    sessions.sumOf { Duration.between(it.startTime, it.endTime).toMinutes() } / 60.0
                }

            // Water: everything logged since midnight
            val water = client.aggregate(
                AggregateRequest(
                    metrics = setOf(HydrationRecord.VOLUME_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startOfToday, now)
                )
            )[HydrationRecord.VOLUME_TOTAL]?.inLiters ?: 0.0

            HealthSnapshot(
                status = HealthStatus.READY,
                sleepLastNight = sleepByDate[today]?.let { Duration.ofMinutes((it * 60).toLong()) },
                waterTodayLiters = water,
                sleepByDate = sleepByDate
            )
        } catch (e: SecurityException) {
            // Permission was taken away between the check and the read
            HealthSnapshot(status = HealthStatus.NEEDS_PERMISSION)
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't read from Health Connect", e)
            HealthSnapshot(status = HealthStatus.NOT_SUPPORTED)
        }
    }
}