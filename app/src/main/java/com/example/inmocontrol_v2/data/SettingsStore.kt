
package com.example.inmocontrol_v2.data
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
val Context.inmoDataStore by preferencesDataStore("inmo_settings")
class SettingsStore private constructor(private val ctx: Context) {
    val autoConnect = ctx.inmoDataStore.data.map { it[AUTO_CONNECT] ?: true }
    val stayConnected = ctx.inmoDataStore.data.map { it[STAY_CONNECTED] ?: true }
    val gyroSensitivity = ctx.inmoDataStore.data.map { it[GYRO_SENS] ?: 8f }
    val scrollSensitivity = ctx.inmoDataStore.data.map { it[SCROLL_SENS] ?: 1.0f }
    val dpadEightWay = ctx.inmoDataStore.data.map { it[DPAD_8] ?: true }
    val gyroBiasX = ctx.inmoDataStore.data.map { it[GYRO_BIAS_X] ?: 0f }
    val gyroBiasY = ctx.inmoDataStore.data.map { it[GYRO_BIAS_Y] ?: 0f }
    suspend fun setAutoConnect(v:Boolean)=ctx.inmoDataStore.edit{ it[AUTO_CONNECT]=v }
    suspend fun setStayConnected(v:Boolean)=ctx.inmoDataStore.edit{ it[STAY_CONNECTED]=v }
    suspend fun setGyroSensitivity(v:Float)=ctx.inmoDataStore.edit{ it[GYRO_SENS]=v }
    suspend fun setScrollSensitivity(v:Float)=ctx.inmoDataStore.edit{ it[SCROLL_SENS]=v }
    suspend fun setDpad8(v:Boolean)=ctx.inmoDataStore.edit{ it[DPAD_8]=v }
    suspend fun setGyroBias(x:Float, y:Float)=ctx.inmoDataStore.edit{ it[GYRO_BIAS_X]=x; it[GYRO_BIAS_Y]=y }
    companion object {
        private val AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        private val STAY_CONNECTED = booleanPreferencesKey("stay_connected")
        private val GYRO_SENS = floatPreferencesKey("gyro_sens")
        private val SCROLL_SENS = floatPreferencesKey("scroll_sens")
        private val DPAD_8 = booleanPreferencesKey("dpad_8")
        private val GYRO_BIAS_X = floatPreferencesKey("gyro_bias_x")
        private val GYRO_BIAS_Y = floatPreferencesKey("gyro_bias_y")
        @Volatile private var inst: SettingsStore? = null
        fun get(ctx: Context): SettingsStore = inst ?: synchronized(this){ inst ?: SettingsStore(ctx.applicationContext).also{ inst = it } }
    }
}
