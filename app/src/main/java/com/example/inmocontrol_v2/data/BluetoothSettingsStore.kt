package com.example.inmocontrol_v2.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.bluetoothSettingsDataStore by preferencesDataStore(name = "bluetooth_settings")

object BluetoothSettingsStore {
    private val AUTO_CONNECT_KEY = booleanPreferencesKey("auto_connect")
    private val LAST_DEVICE_KEY = stringPreferencesKey("last_device_mac")
    private val REMEMBERED_DEVICES_KEY = stringSetPreferencesKey("remembered_devices_mac")

    fun autoConnectFlow(context: Context): Flow<Boolean> =
        context.bluetoothSettingsDataStore.data.map { it[AUTO_CONNECT_KEY] ?: false }

    suspend fun setAutoConnect(context: Context, enabled: Boolean) {
        context.bluetoothSettingsDataStore.edit { it[AUTO_CONNECT_KEY] = enabled }
    }

    fun lastDeviceFlow(context: Context): Flow<String?> =
        context.bluetoothSettingsDataStore.data.map { it[LAST_DEVICE_KEY] }

    suspend fun setLastDevice(context: Context, mac: String) {
        context.bluetoothSettingsDataStore.edit { it[LAST_DEVICE_KEY] = mac }
    }

    fun rememberedDevicesFlow(context: Context): Flow<Set<String>> =
        context.bluetoothSettingsDataStore.data.map { it[REMEMBERED_DEVICES_KEY] ?: emptySet() }

    suspend fun addRememberedDevice(context: Context, mac: String) {
        context.bluetoothSettingsDataStore.edit { prefs ->
            val current = prefs[REMEMBERED_DEVICES_KEY] ?: emptySet()
            prefs[REMEMBERED_DEVICES_KEY] = current + mac
        }
    }

    suspend fun removeRememberedDevice(context: Context, mac: String) {
        context.bluetoothSettingsDataStore.edit { prefs ->
            val current = prefs[REMEMBERED_DEVICES_KEY] ?: emptySet()
            prefs[REMEMBERED_DEVICES_KEY] = current - mac
        }
    }
}
