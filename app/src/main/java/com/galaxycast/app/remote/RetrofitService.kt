package com.galaxycast.app.remote

import com.galaxycast.app.entity.VideoResponse
import kotlinx.coroutines.Deferred
import retrofit2.http.GET
import retrofit2.http.Query


interface RetrofitService {

    @GET("getvideo.php")
    fun getVideo(@Query("url") url: String): Deferred<VideoResponse>
}