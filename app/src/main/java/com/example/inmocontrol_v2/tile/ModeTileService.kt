package com.example.inmocontrol_v2.tile

import androidx.wear.tiles.TileService
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.ModifiersBuilders

/**
 * Fixed Mode Tile Service for quick app access
 */
class ModeTileService : TileService() {

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        return CallbackToFutureAdapter.getFuture { completer ->
            try {
                val tile = createTile()
                completer.set(tile)
            } catch (e: Exception) {
                completer.setException(e)
            }
            "ModeTileRequest"
        }
    }

    override fun onTileResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        return CallbackToFutureAdapter.getFuture { completer ->
            try {
                val resources = ResourceBuilders.Resources.Builder().build()
                completer.set(resources)
            } catch (e: Exception) {
                completer.setException(e)
            }
            "ModeTileResourcesRequest"
        }
    }

    private fun createTile(): TileBuilders.Tile {
        // Create launch action
        val androidActivity = ActionBuilders.AndroidActivity.Builder()
            .setPackageName(packageName)
            .setClassName("com.example.inmocontrol_v2.MainActivity")
            .build()

        val launchAction = ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(androidActivity)
            .build()

        // Create clickable text
        val text = LayoutElementBuilders.Text.Builder()
            .setText("InmoControl")
            .build()

        val layout = LayoutElementBuilders.Box.Builder()
            .addContent(text)
            .build()

        val timeline = TimelineBuilders.Timeline.Builder()
            .addTimelineEntry(
                TimelineBuilders.TimelineEntry.Builder()
                    .setLayout(
                        LayoutElementBuilders.Layout.Builder()
                            .setRoot(layout)
                            .build()
                    )
                    .build()
            )
            .build()

        return TileBuilders.Tile.Builder()
            .setResourcesVersion("1")
            .setTileTimeline(timeline)
            .build()
    }
}