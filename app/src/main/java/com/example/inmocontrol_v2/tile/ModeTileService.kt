package com.example.inmocontrol_v2.tile

import androidx.wear.tiles.TileService
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture

class ModeTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        return CallbackToFutureAdapter.getFuture { completer ->
            val tile = TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
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