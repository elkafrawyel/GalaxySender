package com.galaxycast.app.useCase

import com.galaxycast.app.entity.DataResource
import com.galaxycast.app.entity.Video
import com.galaxycast.app.entity.toVideo
import com.galaxycast.app.remote.RetrofitService

class GetVideoUseCase(private val retrofitService: RetrofitService) {
    suspend fun getVideo(videoId: String): DataResource<Video> {
        val videoResponse = retrofitService.getVideo("https://www.youtube.com/watch?v=$videoId").await()
        if (videoResponse.status == true) {
            val video = videoResponse.toVideo()
            if (video != null) {
                return DataResource.Success(video!!)
            } else {
                return DataResource.Error("Failed to get video info.")
            }
        } else {
            return DataResource.Error("Failed to get video info.")
        }
    }
}