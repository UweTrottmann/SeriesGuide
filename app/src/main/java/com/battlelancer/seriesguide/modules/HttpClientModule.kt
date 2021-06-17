package com.battlelancer.seriesguide.modules

import android.content.Context
import android.os.StatFs
import com.battlelancer.seriesguide.tmdbapi.SgTmdbInterceptor
import com.battlelancer.seriesguide.traktapi.SgTraktInterceptor
import com.battlelancer.seriesguide.util.AllApisAuthenticator
import dagger.Module
import dagger.Provides
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
open class HttpClientModule {

    /**
     * Returns this apps [OkHttpClient] with enabled response cache. Should be used with API calls.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        cache: Cache,
        traktInterceptor: SgTraktInterceptor,
        tmdbInterceptor: SgTmdbInterceptor,
        authenticator: AllApisAuthenticator
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
        builder.connectTimeout(CONNECT_TIMEOUT_MILLIS.toLong(), TimeUnit.MILLISECONDS)
        builder.readTimeout(READ_TIMEOUT_MILLIS.toLong(), TimeUnit.MILLISECONDS)
        builder.addInterceptor(tmdbInterceptor)
        builder.addInterceptor(traktInterceptor)
        builder.authenticator(authenticator)
        builder.cache(cache)
        return builder.build()
    }

    @Provides
    @Singleton
    open fun provideOkHttpCache(@ApplicationContext context: Context): Cache {
        val cacheDir = createApiCacheDir(context, API_CACHE)
        return Cache(cacheDir, calculateApiDiskCacheSize(cacheDir))
    }

    companion object {
        private const val CONNECT_TIMEOUT_MILLIS = 15 * 1000 // 15s
        private const val READ_TIMEOUT_MILLIS = 20 * 1000 // 20s

        const val API_CACHE = "api-cache"
        private const val MIN_DISK_API_CACHE_SIZE = 2 * 1024 * 1024L // 2MB
        private const val MAX_DISK_API_CACHE_SIZE = 20 * 1024 * 1024L // 20MB

        fun createApiCacheDir(context: Context, directoryName: String): File {
            val cache = File(context.cacheDir, directoryName)
            if (!cache.exists()) {
                cache.mkdirs()
            }
            return cache
        }

        fun calculateApiDiskCacheSize(dir: File): Long {
            val size = try {
                val statFs = StatFs(dir.absolutePath)
                val available = statFs.blockCountLong * statFs.blockSizeLong
                // Target 2% of the total space.
                available / 50
            } catch (ignored: IllegalArgumentException) {
                MIN_DISK_API_CACHE_SIZE
            }

            // Bound inside min/max size for disk cache.
            return size.coerceIn(MIN_DISK_API_CACHE_SIZE, MAX_DISK_API_CACHE_SIZE)
        }
    }
}