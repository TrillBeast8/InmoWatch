package com.example.inmocontrol_v2.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore private constructor(private val context: Context) {
    companion object {
        private val KEYBOARD_MODE = booleanPreferencesKey("keyboard_mode")
        private val MOUSE_MODE = booleanPreferencesKey("mouse_mode")
        private val TOUCHPAD_MODE = booleanPreferencesKey("touchpad_mode")
        private val DPAD_MODE = booleanPreferencesKey("dpad_mode")
        private val MEDIA_MODE = booleanPreferencesKey("media_mode")
        private val DPAD_EIGHT_WAY = booleanPreferencesKey("dpad_eight_way")

        @Volatile
        private var INSTANCE: SettingsStore? = null

        fun get(context: Context): SettingsStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsStore(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val keyboardMode: Flow<Boolean> = context.settingsDataStore.data.map { it[KEYBOARD_MODE] ?: false }
    val mouseMode: Flow<Boolean> = context.settingsDataStore.data.map { it[MOUSE_MODE] ?: false }
    val touchpadMode: Flow<Boolean> = context.settingsDataStore.data.map { it[TOUCHPAD_MODE] ?: false }
    val dpadMode: Flow<Boolean> = context.settingsDataStore.data.map { it[DPAD_MODE] ?: false }
    val mediaMode: Flow<Boolean> = context.settingsDataStore.data.map { it[MEDIA_MODE] ?: false }
    val dpadEightWay: Flow<Boolean> = context.settingsDataStore.data.map { it[DPAD_EIGHT_WAY] ?: true }

    suspend fun setKeyboardMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEYBOARD_MODE] = enabled }
    }
    suspend fun setMouseMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[MOUSE_MODE] = enabled }
    }
    suspend fun setTouchpadMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[TOUCHPAD_MODE] = enabled }
    }
    suspend fun setDpadMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[DPAD_MODE] = enabled }
    }
    suspend fun setMediaMode(enabled: Boolean) {
        context.settingsDataStore.edit { it[MEDIA_MODE] = enabled }
    }
    suspend fun setDpadEightWay(enabled: Boolean) {
        context.settingsDataStore.edit { it[DPAD_EIGHT_WAY] = enabled }
    }
}