package com.castgalaxy.app

import android.app.Application
import com.blankj.utilcode.util.Utils
import com.castgalaxy.app.remote.RetrofitService
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.youtube.YouTubeScopes
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class GalaxyCastApplication : Application() {


    override fun onCreate() {
        super.onCreate()
        instance = this
        Utils.init(this)
    }

    companion object {
        lateinit var instance: GalaxyCastApplication
            private set

        private val SCOPES = arrayOf(YouTubeScopes.YOUTUBE)

        val mCredential: GoogleAccountCredential by lazy {
            GoogleAccountCredential.usingOAuth2(
                instance, SCOPES.toCollection(mutableListOf())
            ).setBackOff(ExponentialBackOff())
        }

        fun retrofitService(): RetrofitService {
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY

            val okHttpClient = OkHttpClient
                .Builder()
                .addInterceptor(interceptor)
                .build()

            val retrofit = Retrofit.Builder()
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create())
                .baseUrl("http://galaxycast.hmaserv.online/")
                .build()

            return retrofit.create(RetrofitService::class.java)
        }
    }
}