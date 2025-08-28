
package com.example.inmocontrol_v2.boot
import android.content.*; import com.example.inmocontrol_v2.hid.HidService
class BootReceiver: BroadcastReceiver(){ override fun onReceive(c: Context, i: Intent){ if (Intent.ACTION_BOOT_COMPLETED==i.action){ c.startForegroundService(Intent(c, HidService::class.java)) } } }
