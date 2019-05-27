package com.castgalaxy.app.remote

import com.castgalaxy.app.entity.LoginResponse
import com.castgalaxy.app.entity.VideoResponse
import kotlinx.coroutines.Deferred
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query


interface RetrofitService {

    @GET("getvideo.php")
    fun getVideo(@Query("url") url: String): Deferred<VideoResponse>

    @GET("activecode.php")
    fun login(
        @Query("activecode") code: String,
        @Query("uid") uId: String,
        @Query("versioncode") version :String): Call<LoginResponse>


}