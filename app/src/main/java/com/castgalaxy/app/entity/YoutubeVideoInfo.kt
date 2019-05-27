package com.castgalaxy.app.entity

data class YoutubeVideoInfo(
    var format: String,
    var url: String
){
    override fun toString(): String {
        return format
    }
}