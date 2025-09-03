package com.example.inmocontrol_v2.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.inmocontrol_v2.hid.HidService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            context.startForegroundService(Intent(context, HidService::class.java))
        }
    }
}
