package com.example.qhagoapp

import java.io.File
import android.app.Application
import org.osmdroid.config.Configuration

class Thruxion : Application()
{
    override fun onCreate()
    {
        super.onCreate()

        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "tiles")
            tileFileSystemCacheMaxBytes = 50L * 1024 * 1024
            tileFileSystemCacheTrimBytes = 40L * 1024 * 1024
        }
    }

}