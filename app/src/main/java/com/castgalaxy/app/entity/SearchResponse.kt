package com.castgalaxy.app.entity
import com.squareup.moshi.Json


data class SearchResponse(
    @field:Json(name = "status")
    val status: String?,
    @field:Json(name = "nextpagetoken")
    val nextpagetoken: String?,
    @field:Json(name = "prevPageToken")
    val prevPageToken: Any?,
    @field:Json(name = "data")
    val `data`: List<YoutubeVideoResponse?>?
)

data class YoutubeVideoResponse(
    @field:Json(name = "videoid")
    val videoid: String?,
    @field:Json(name = "videotitle")
    val videotitle: String?,
    @field:Json(name = "Channeltitle")
    val channeltitle: String?,
    @field:Json(name = "description")
    val description: String?,
    @field:Json(name = "duration")
    val duration: String?,
    @field:Json(name = "quality")
    val quality: String?,
    @field:Json(name = "islicensed")
    val islicensed: Boolean?,
    @field:Json(name = "videodate")
    val videodate: String?,
    @field:Json(name = "imgmedium")
    val imgmedium: String?,
    @field:Json(name = "imgdefault")
    val imgdefault: String?,
    @field:Json(name = "imghigh")
    val imghigh: String?
)