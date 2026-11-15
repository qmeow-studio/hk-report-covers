package com.tx.reportcovers

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory

class App : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        DataSource.init(this)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient { DataSource.client }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
}
