package com.castgalaxy.app.entity

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id

@Entity
data class MyVideos(
    @Id
    var id: Long = 0,
    var videoId:String,
    var title: String,
    var channelName: String,
    var time: Long,
    var image: String
)