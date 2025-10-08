package com.example.inmocontrol_v2.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Optimized SettingsStore with improved memory efficiency and performance
 */
class SettingsStore private constructor(context: Context) {
    // Use application context to prevent memory leaks
    private val dataStore = context.applicationContext.settingsDataStore

    companion object {
        // Grouped preference keys for better organization
        private val SENSITIVITY = floatPreferencesKey("sensitivity")
        private val REMOTE_BACK_DOUBLE_CLICK = booleanPreferencesKey("remote_back_double_click")
        private val REALTIME_KEYBOARD = booleanPreferencesKey("realtime_keyboard")
        private val SCROLL_SENSITIVITY = floatPreferencesKey("scroll_sensitivity")
        private val MOUSE_CALIBRATION_COMPLETE = booleanPreferencesKey("mouse_calibration_complete")

        @Volatile
        private var INSTANCE: SettingsStore? = null

        fun get(context: Context): SettingsStore =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsStore(context).also { INSTANCE = it }
            }
    }

    // Optimized flow properties with distinctUntilChanged for better performance
    val sensitivity: Flow<Float> = dataStore.data
        .map { it[SENSITIVITY] ?: 1.0f }
        .distinctUntilChanged()

    val remoteBackDoubleClick: Flow<Boolean> = dataStore.data
        .map { it[REMOTE_BACK_DOUBLE_CLICK] ?: false }
        .distinctUntilChanged()

    val realtimeKeyboard: Flow<Boolean> = dataStore.data
        .map { it[REALTIME_KEYBOARD] ?: false }
        .distinctUntilChanged()

    val scrollSensitivity: Flow<Float> = dataStore.data
        .map { it[SCROLL_SENSITIVITY] ?: 1.0f }
        .distinctUntilChanged()

    val mouseCalibrationComplete: Flow<Boolean> = dataStore.data
        .map { it[MOUSE_CALIBRATION_COMPLETE] ?: false }
        .distinctUntilChanged()

    // Efficient update methods
    suspend fun setSensitivity(value: Float) {
        dataStore.edit { it[SENSITIVITY] = value }
    }

    suspend fun setRemoteBackDoubleClick(enabled: Boolean) {
        dataStore.edit { it[REMOTE_BACK_DOUBLE_CLICK] = enabled }
    }

    suspend fun setRealtimeKeyboard(enabled: Boolean) {
        dataStore.edit { it[REALTIME_KEYBOARD] = enabled }
    }

    suspend fun setScrollSensitivity(value: Float) {
        dataStore.edit { it[SCROLL_SENSITIVITY] = value }
    }

    suspend fun setMouseCalibrationComplete(enabled: Boolean) {
        dataStore.edit { it[MOUSE_CALIBRATION_COMPLETE] = enabled }
    }

    suspend fun saveMouseCalibrationComplete(complete: Boolean) {
        setMouseCalibrationComplete(complete)
    }
}