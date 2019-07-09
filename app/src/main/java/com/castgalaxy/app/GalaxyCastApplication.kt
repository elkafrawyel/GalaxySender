package com.castgalaxy.app

import android.app.Application
import com.blankj.utilcode.util.Utils
import com.castgalaxy.app.remote.RetrofitService
import com.castgalaxy.app.utily.ObjectBox
import com.castgalaxy.app.local.PreferencesHelper
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.youtube.YouTubeScopes
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class GalaxyCastApplication : Application() {


    override fun onCreate() {
        super.onCreate()
        instance = this
        Utils.init(this)
        ObjectBox.init(this)
    }

    companion object {
        lateinit var instance: GalaxyCastApplication
            private set

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

        fun getPreferenceHelper() = PreferencesHelper(instance)

    }
}