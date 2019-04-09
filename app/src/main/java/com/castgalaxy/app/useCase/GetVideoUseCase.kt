package com.castgalaxy.app.useCase

import com.castgalaxy.app.entity.DataResource
import com.castgalaxy.app.entity.Video
import com.castgalaxy.app.entity.toVideo
import com.castgalaxy.app.remote.RetrofitService
import com.castgalaxy.app.ui.player.PlayerActivity

class GetVideoUseCase(private val retrofitService: RetrofitService) {
    suspend fun getVideo(id_or_url: String, type: PlayerActivity.VideoType): DataResource<Video> {
        val link: String = when (type) {
            PlayerActivity.VideoType.VIDEO -> "https://www.youtube.com/watch?v=$id_or_url"
            PlayerActivity.VideoType.URL -> id_or_url
        }
        val videoResponse = retrofitService.getVideo(link).await()
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