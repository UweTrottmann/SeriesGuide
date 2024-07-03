// SPDX-License-Identifier: Apache-2.0
// Copyright 2016-2024 Uwe Trottmann

package com.battlelancer.seriesguide.modules

import android.content.Context
import android.os.Build
import android.os.StatFs
import com.battlelancer.seriesguide.tmdbapi.SgTmdbInterceptor
import com.battlelancer.seriesguide.traktapi.SgTraktInterceptor
import com.battlelancer.seriesguide.util.AllApisAuthenticator
import dagger.Module
import dagger.Provides
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.decodeCertificatePem
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
        builder.trustLetsEncryptAndroidNOrLower()
        return builder.build()
    }

    @Provides
    @Singleton
    open fun provideOkHttpCache(@ApplicationContext context: Context): Cache {
        return getApiDiskCache(context)
    }

    companion object {
        private const val CONNECT_TIMEOUT_MILLIS = 15 * 1000 // 15s
        private const val READ_TIMEOUT_MILLIS = 20 * 1000 // 20s

        const val API_CACHE = "api-cache"
        private const val MIN_DISK_API_CACHE_SIZE = 2 * 1024 * 1024L // 2MB
        private const val MAX_DISK_API_CACHE_SIZE = 20 * 1024 * 1024L // 20MB

        private const val IMAGE_CACHE = "picasso-cache"
        private const val MIN_DISK_IMAGE_CACHE_SIZE = 5 * 1024 * 1024L // 5MB
        private const val MAX_DISK_IMAGE_CACHE_SIZE = 50 * 1024 * 1024L // 50MB

        // https://letsencrypt.org/certs/isrgrootx1.pem
        private val letsEncryptIsgX1Cert = """
        -----BEGIN CERTIFICATE-----
        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
        -----END CERTIFICATE-----
        """.trimIndent()

        // Keep global to reduce memory footprint of loading all trusted certificates
        private val certificates by lazy {
            val letsEncryptCert = letsEncryptIsgX1Cert.decodeCertificatePem()

            HandshakeCertificates.Builder()
                .addTrustedCertificate(letsEncryptCert)
                // Trakt serves images rooted in a Google Trust Services certificate,
                // also in case TMDB ever changes their certificate provider,
                // add platform certificates even as this does increase memory consumption.
                .addPlatformTrustedCertificates()
                .build()
        }

        /**
         * SeriesGuide image cache server and TMDB (image) servers use Let's Encrypt:
         * the Let's Encrypt X1 root certificate is not trusted before Android 7.1.1
         * and cross-signing with a trusted certificate has stopped June 2024,
         * so customize to also trust their X1 root certificate.
         *
         * - [Let's Encrypt blog post](https://letsencrypt.org/2023/07/10/cross-sign-expiration.html)
         * - [OkHttp custom trust recipe](https://github.com/square/okhttp/blob/master/samples/guide/src/main/java/okhttp3/recipes/kt/CustomTrust.kt)
         */
        fun OkHttpClient.Builder.trustLetsEncryptAndroidNOrLower(): OkHttpClient.Builder {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                sslSocketFactory(certificates.sslSocketFactory(), certificates.trustManager)
            }
            return this
        }

        fun createCacheDir(context: Context, directoryName: String): File {
            val cache = File(context.cacheDir, directoryName)
            if (!cache.exists()) {
                cache.mkdirs()
            }
            return cache
        }

        private fun calculateDiskCacheSize(dir: File, minSize: Long, maxSize: Long): Long {
            val size = try {
                val statFs = StatFs(dir.absolutePath)
                val available = statFs.blockCountLong * statFs.blockSizeLong
                // Target 2% of the total space.
                available / 50
            } catch (ignored: IllegalArgumentException) {
                minSize
            }
            // Bound inside min/max size for disk cache.
            return size.coerceIn(minSize, maxSize)
        }

        fun calculateApiDiskCacheSize(dir: File): Long {
            return calculateDiskCacheSize(dir, MIN_DISK_API_CACHE_SIZE, MAX_DISK_API_CACHE_SIZE)
        }

        fun getApiDiskCache(context: Context): Cache {
            val cacheDir = createCacheDir(context, API_CACHE)
            return Cache(cacheDir, calculateApiDiskCacheSize(cacheDir))
        }

        fun getImageDiskCache(context: Context): Cache {
            val cacheDir = createCacheDir(context, IMAGE_CACHE)
            return Cache(
                cacheDir,
                calculateDiskCacheSize(
                    cacheDir,
                    MIN_DISK_IMAGE_CACHE_SIZE,
                    MAX_DISK_IMAGE_CACHE_SIZE
                )
            )
        }
    }
}