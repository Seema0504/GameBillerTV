package com.gamebiller.tvlock.di

import android.content.Context
import com.gamebiller.tvlock.BuildConfig
import com.gamebiller.tvlock.data.remote.ApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module providing network-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    /**
     * Provide Moshi JSON converter
     */
    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(
                com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory.of(com.gamebiller.tvlock.domain.model.AuditMetadata::class.java, "type")
                    .withSubtype(com.gamebiller.tvlock.domain.model.AuditMetadata.NetworkLost::class.java, "NETWORK_LOST")
                    .withSubtype(com.gamebiller.tvlock.domain.model.AuditMetadata.GracePeriodExpired::class.java, "GRACE_EXPIRED")
                    .withSubtype(com.gamebiller.tvlock.domain.model.AuditMetadata.ManualLock::class.java, "MANUAL_LOCK")
                    .withSubtype(com.gamebiller.tvlock.domain.model.AuditMetadata.AppRestarted::class.java, "APP_RESTARTED")
                    .withSubtype(com.gamebiller.tvlock.domain.model.AuditMetadata.DevicePaired::class.java, "DEVICE_PAIRED")
                    .withSubtype(com.gamebiller.tvlock.domain.model.AuditMetadata.Generic::class.java, "GENERIC")
            )
            .add(KotlinJsonAdapterFactory())
            .build()
    }
    
    /**
     * Provide OkHttp client with logging and timeouts
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
        
        // Add logging interceptor in debug builds
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor { message ->
                Timber.tag("OkHttp").d(message)
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }
        
        return builder.build()
    }
    
    /**
     * Provide Retrofit instance
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
    
    /**
     * Provide ApiService
     */
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
    
    /**
     * Provide Application Context
     */
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
}
