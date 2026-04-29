package com.safistep.di

import android.content.Context
import androidx.room.Room
import com.safistep.BuildConfig
import com.safistep.data.local.SafiStepPreferences
import com.safistep.data.local.dao.BlockHistoryDao
import com.safistep.data.local.dao.PlatformDao
import com.safistep.data.local.dao.SyncMetaDao
import com.safistep.data.local.db.SafiStepDatabase
import com.safistep.data.remote.api.SafiStepApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

// ── Network Module ────────────────────────────────────────────
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides @Singleton
    fun provideAuthInterceptor(prefs: SafiStepPreferences): Interceptor = Interceptor { chain ->
        val token = runBlocking { prefs.getTokenSync() }
        val request = chain.request().newBuilder().apply {
            header("Accept", "application/json")
            header("X-App-Version", BuildConfig.APP_VERSION)
            if (!token.isNullOrBlank()) {
                header("Authorization", "Bearer $token")
            }
        }.build()
        chain.proceed(request)
    }

    @Provides @Singleton
   fun provideOkHttp(authInterceptor: Interceptor): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY  // force this even in release
        })
        .addInterceptor { chain ->
            try {
                chain.proceed(chain.request())
            } catch (e: Exception) {
                android.util.Log.e("OkHttp", "Request failed: ${e::class.java.name}: ${e.message}")
                throw e
            }
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
   }

    @Provides @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides @Singleton
    fun provideApi(retrofit: Retrofit): SafiStepApi = retrofit.create(SafiStepApi::class.java)
}

// ── Database Module ───────────────────────────────────────────
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SafiStepDatabase =
        Room.databaseBuilder(context, SafiStepDatabase::class.java, "safistep.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun providePlatformDao(db: SafiStepDatabase): PlatformDao = db.platformDao()
    @Provides fun provideBlockHistoryDao(db: SafiStepDatabase): BlockHistoryDao = db.blockHistoryDao()
    @Provides fun provideSyncMetaDao(db: SafiStepDatabase): SyncMetaDao = db.syncMetaDao()
}
