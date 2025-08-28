
package com.example.inmocontrol_v2.hid
import android.app.Application; import android.content.Intent
object HidClient { private var service:HidService?=null
    fun init(app: Application){ app.startForegroundService(Intent(app,HidService::class.java)); HidService.setClientListener{ service=it } }
    fun instance(): HidService? = service }
