package com.example.inmocontrol_v2.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore private constructor(private val context: Context) {
    companion object {
        private val SENSITIVITY = floatPreferencesKey("sensitivity")
        private val REMOTE_BACK_DOUBLE_CLICK = booleanPreferencesKey("remote_back_double_click")
        private val BASELINE_GYRO_X = floatPreferencesKey("baseline_gyro_x")
        private val BASELINE_GYRO_Y = floatPreferencesKey("baseline_gyro_y")
        private val BASELINE_GYRO_Z = floatPreferencesKey("baseline_gyro_z")
        private val BASELINE_ACCEL_X = floatPreferencesKey("baseline_accel_x")
        private val BASELINE_ACCEL_Y = floatPreferencesKey("baseline_accel_y")
        private val BASELINE_ACCEL_Z = floatPreferencesKey("baseline_accel_z")
        private val CALIBRATION_MIN_X = floatPreferencesKey("calibration_min_x")
        private val CALIBRATION_MIN_Y = floatPreferencesKey("calibration_min_y")
        private val CALIBRATION_MAX_X = floatPreferencesKey("calibration_max_x")
        private val CALIBRATION_MAX_Y = floatPreferencesKey("calibration_max_y")
        private val CALIBRATION_CENTER_X = floatPreferencesKey("calibration_center_x")
        private val CALIBRATION_CENTER_Y = floatPreferencesKey("calibration_center_y")

        @Volatile
        private var INSTANCE: SettingsStore? = null

        fun get(context: Context): SettingsStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val sensitivity: Flow<Float> = context.settingsDataStore.data.map { it[SENSITIVITY] ?: 0.5f }
    val remoteBackDoubleClick: Flow<Boolean> = context.settingsDataStore.data.map { it[REMOTE_BACK_DOUBLE_CLICK] ?: false }
    val baselineGyro: Flow<FloatArray> = context.settingsDataStore.data.map {
        floatArrayOf(
            it[BASELINE_GYRO_X] ?: 0f,
            it[BASELINE_GYRO_Y] ?: 0f,
            it[BASELINE_GYRO_Z] ?: 0f
        )
    }
    val baselineAccel: Flow<FloatArray> = context.settingsDataStore.data.map {
        floatArrayOf(
            it[BASELINE_ACCEL_X] ?: 0f,
            it[BASELINE_ACCEL_Y] ?: 0f,
            it[BASELINE_ACCEL_Z] ?: 0f
        )
    }
    suspend fun setSensitivity(value: Float) {
        context.settingsDataStore.edit { it[SENSITIVITY] = value }
    }
    suspend fun setRemoteBackDoubleClick(enabled: Boolean) {
        context.settingsDataStore.edit { it[REMOTE_BACK_DOUBLE_CLICK] = enabled }
    }
    suspend fun setBaselineGyro(values: FloatArray) {
        context.settingsDataStore.edit {
            it[BASELINE_GYRO_X] = values.getOrNull(0) ?: 0f
            it[BASELINE_GYRO_Y] = values.getOrNull(1) ?: 0f
            it[BASELINE_GYRO_Z] = values.getOrNull(2) ?: 0f
        }
    }
    suspend fun setBaselineAccel(values: FloatArray) {
        context.settingsDataStore.edit {
            it[BASELINE_ACCEL_X] = values.getOrNull(0) ?: 0f
            it[BASELINE_ACCEL_Y] = values.getOrNull(1) ?: 0f
            it[BASELINE_ACCEL_Z] = values.getOrNull(2) ?: 0f
        }
    }
    suspend fun saveCalibration(
        minX: Float,
        minY: Float,
        maxX: Float,
        maxY: Float,
        centerX: Float,
        centerY: Float,
        sensitivity: Float
    ) {
        context.settingsDataStore.edit {
            it[CALIBRATION_MIN_X] = minX
            it[CALIBRATION_MIN_Y] = minY
            it[CALIBRATION_MAX_X] = maxX
            it[CALIBRATION_MAX_Y] = maxY
            it[CALIBRATION_CENTER_X] = centerX
            it[CALIBRATION_CENTER_Y] = centerY
            it[SENSITIVITY] = sensitivity
        }
    }
}