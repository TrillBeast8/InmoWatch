package com.example.inmocontrol_v2.data

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * BluetoothSettingsStore - Manages Bluetooth device preferences and last connected device
 * Preserving original functionality for reconnect feature
 */
object BluetoothSettingsStore {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bluetooth_settings")

    private val LAST_DEVICE_ADDRESS_KEY = stringPreferencesKey("last_device_address")
    private val LAST_DEVICE_NAME_KEY = stringPreferencesKey("last_device_name")

    /**
     * Save the last connected device for quick reconnection
     */
    suspend fun saveLastDevice(context: Context, device: BluetoothDevice) {
        try {
            context.dataStore.edit { preferences ->
                preferences[LAST_DEVICE_ADDRESS_KEY] = device.address
                preferences[LAST_DEVICE_NAME_KEY] = device.name ?: "Unknown Device"
            }
        } catch (e: SecurityException) {
            // Handle permission issues gracefully
            context.dataStore.edit { preferences ->
                preferences[LAST_DEVICE_ADDRESS_KEY] = device.address
                preferences[LAST_DEVICE_NAME_KEY] = "Bluetooth Device"
            }
        }
    }

    /**
     * Get flow of last connected device for reactive UI updates
     * Returns null if no device was previously connected
     */
    fun lastDeviceFlow(context: Context): Flow<BluetoothDevice?> {
        return context.dataStore.data.map { preferences ->
            val address = preferences[LAST_DEVICE_ADDRESS_KEY]

            if (address != null) {
                try {
                    // Get the real BluetoothDevice from the adapter using the saved address
                    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                    val bluetoothAdapter = bluetoothManager.adapter
                    bluetoothAdapter?.getRemoteDevice(address)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }

    /**
     * Clear the last connected device
     */
    suspend fun clearLastDevice(context: Context) {
        context.dataStore.edit { preferences ->
            preferences.remove(LAST_DEVICE_ADDRESS_KEY)
            preferences.remove(LAST_DEVICE_NAME_KEY)
        }
    }
}
