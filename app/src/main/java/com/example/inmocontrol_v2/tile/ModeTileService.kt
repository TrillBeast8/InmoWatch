
package com.example.inmocontrol_v2.tile
import android.app.PendingIntent; import android.content.Intent
import androidx.wear.tiles.*; import androidx.wear.tiles.material.layouts.MultiButtonLayout; import androidx.wear.tiles.material.Button; import androidx.wear.tiles.ModifiersBuilders
import com.example.inmocontrol_v2.MainActivity
class ModeTileService: TileService(){
    override fun onTileRequest(p: RequestBuilders.TileRequest): TileBuilders.Tile {
        val labels=listOf("Keyboard","Touchpad","Mouse","D-Pad","Media")
        val buttons=labels.map{ Button.Builder(this, clickable(it)).setTextContent(it).build() }
        val layout=MultiButtonLayout.Builder().apply{ buttons.forEach{ addButtonContent(it) } }.build()
        return TileBuilders.Tile.Builder().setResourcesVersion("1").setTimeline(TileBuilders.Timeline.Builder().addTimelineEntry(TileBuilders.TimelineEntry.Builder().setLayout(TileBuilders.Layout.Builder().setRoot(layout).build()).build()).build()).build()
    }
    private fun clickable(label:String): ModifiersBuilders.Clickable {
        val route=when(label){"Keyboard"->"keyboard";"Touchpad"->"touchpad";"Mouse"->"mouse";"D-Pad"->"dpad";else->"media"}
        val i=Intent(this, MainActivity::class.java).apply{ action=Intent.ACTION_VIEW; data=android.net.Uri.parse("inmo://open?route=$route"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        val pi=PendingIntent.getActivity(this, route.hashCode(), i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return ModifiersBuilders.Clickable.Builder().setId(route).setOnClick(pi).build()
    }
}
