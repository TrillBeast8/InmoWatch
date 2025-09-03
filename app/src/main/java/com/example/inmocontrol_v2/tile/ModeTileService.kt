package com.example.inmocontrol_v2.tile

import androidx.wear.tiles.TileService
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.ActionBuilders
import android.content.Intent
import android.app.PendingIntent
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.wear.tiles.material.Text
import androidx.wear.tiles.material.layouts.PrimaryLayout
import androidx.wear.tiles.DimensionBuilders
import androidx.wear.tiles.TimelineBuilders
import androidx.wear.tiles.ModifiersBuilders

class ModeTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        return CallbackToFutureAdapter.getFuture { completer ->
            val intent = Intent(this, com.example.inmocontrol_v2.MainActivity::class.java)
            val androidActivity = ActionBuilders.AndroidActivity.Builder()
                .setPackageName(packageName)
                .setClassName(com.example.inmocontrol_v2.MainActivity::class.java.name)
                .build()
            val launchAction = ActionBuilders.LaunchAction.Builder()
                .setAndroidActivity(androidActivity)
                .build()
            val text = LayoutElementBuilders.Text.Builder()
                .setText("Inmo Controller")
                .setFontStyle(
                    LayoutElementBuilders.FontStyle.Builder()
                        .setSize(DimensionBuilders.SpProp.Builder().setValue(20f).build())
                        .build()
                )
                .build()
            val box = LayoutElementBuilders.Box.Builder()
                .addContent(text)
                .setModifiers(
                    ModifiersBuilders.Modifiers.Builder()
                        .setClickable(
                            ModifiersBuilders.Clickable.Builder()
                                .setId("launch_app")
                                .setOnClick(launchAction)
                                .build()
                        )
                        .build()
                )
                .build()
            val layout = LayoutElementBuilders.Layout.Builder()
                .setRoot(box)
                .build()
            val timelineEntry = TimelineBuilders.TimelineEntry.Builder()
                .setLayout(layout)
                .build()
            val timeline = TimelineBuilders.Timeline.Builder()
                .addTimelineEntry(timelineEntry)
                .build()
            val tile = TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setTimeline(timeline)
                .build()
            completer.set(tile)
            "TileRequest"
        }
    }

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        return CallbackToFutureAdapter.getFuture { completer ->
            val resources = ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .build()
            completer.set(resources)
            "ResourcesRequest"
        }
    }


    companion object {
        private const val RESOURCES_VERSION = "1"
    }
}