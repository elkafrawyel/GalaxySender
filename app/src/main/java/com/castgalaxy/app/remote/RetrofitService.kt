package com.castgalaxy.app.remote

import com.castgalaxy.app.entity.VideoResponse
import kotlinx.coroutines.Deferred
import retrofit2.http.GET
import retrofit2.http.Query


interface RetrofitService {

    @GET("getvideo.php")
    fun getVideo(@Query("urlList") url: String): Deferred<VideoResponse>
}