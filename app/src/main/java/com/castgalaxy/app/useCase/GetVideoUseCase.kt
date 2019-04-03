package com.castgalaxy.app.useCase

import com.castgalaxy.app.entity.DataResource
import com.castgalaxy.app.entity.Video
import com.castgalaxy.app.entity.toVideo
import com.castgalaxy.app.remote.RetrofitService

class GetVideoUseCase(private val retrofitService: RetrofitService) {
    suspend fun getVideo(videoId: String): DataResource<Video> {
        val videoResponse = retrofitService.getVideo("https://www.youtube.com/watch?v=$videoId").await()
        return if (videoResponse.status == true) {
            val video = videoResponse.toVideo()
            if (video != null) {
                DataResource.Success(video)
            } else {
                DataResource.Error("Failed to get video info.")
            }
        } else {
            DataResource.Error("Failed to get video info.")
        }
    }
}