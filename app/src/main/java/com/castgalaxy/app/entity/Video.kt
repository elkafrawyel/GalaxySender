package com.castgalaxy.app.entity

import com.squareup.moshi.Json

data class VideoResponse(
    @field:Json(name = "status")
    val status: Boolean?,
    @field:Json(name = "message")
    val message: String?,
    @field:Json(name = "description")
    val description: String?,
    @field:Json(name = "uploader")
    val uploader: String?,
    @field:Json(name = "urlList")
    val url: String?,
    @field:Json(name = "id")
    val id: String?,
    @field:Json(name = "is_playlist")
    val isPlaylist: Boolean?,
    @field:Json(name = "site")
    val site: String?,
    @field:Json(name = "title")
    val title: String?,
    @field:Json(name = "like_count")
    val likeCount: Int?,
    @field:Json(name = "dislike_count")
    val dislikeCount: Int?,
    @field:Json(name = "view_count")
    val viewCount: Int?,
    @field:Json(name = "duration")
    val duration: Int?,
    @field:Json(name = "upload_date")
    val uploadDate: String?,
    @field:Json(name = "tags")
    val tags: List<String?>?,
    @field:Json(name = "uploader_url")
    val uploaderUrl: String?,
    @field:Json(name = "thumbnail")
    val thumbnail: String?,
    @field:Json(name = "streams")
    val streams: List<StreamResponse?>?
)

data class Video(
    val uploader: String,
    val url: String,
    val id: String,
    val title: String,
    val thumbnail: String,
    val streams: List<Stream>
)

fun VideoResponse.toVideo(): Video? {
    if (uploader != null &&
        url != null &&
        id != null &&
        title != null &&
        thumbnail != null &&
        streams != null
    ) {
        return Video(uploader, url, id, title, thumbnail, streams.mapNotNull { it?.toStream() })
    }
    return null
}

data class StreamResponse(
    @field:Json(name = "urlList")
    val url: String?,
    @field:Json(name = "format")
    val format: String?,
    @field:Json(name = "format_note")
    val formatNote: String?,
    @field:Json(name = "extension")
    val extension: String?,
    @field:Json(name = "video_codec")
    val videoCodec: String?,
    @field:Json(name = "audio_codec")
    val audioCodec: String?,
    @field:Json(name = "height")
    val height: Int?,
    @field:Json(name = "width")
    val width: Int?,
    @field:Json(name = "fps")
    val fps: Int?,
    @field:Json(name = "fmt_id")
    val fmtId: String?,
    @field:Json(name = "filesize")
    val filesize: Int?,
    @field:Json(name = "filesize_pretty")
    val filesizePretty: String?,
    @field:Json(name = "has_audio")
    val hasAudio: Boolean?,
    @field:Json(name = "has_video")
    val hasVideo: Boolean?,
    @field:Json(name = "is_hd")
    val isHd: Boolean?
)

data class Stream(
    val url: String,
    val format: String,
    val hasAudio: Boolean,
    val hasVideo: Boolean,
    val isHd: Boolean,
    val extension: String
)

fun StreamResponse.toStream(): Stream? {
    if (url != null &&
        hasVideo != null &&
        hasAudio != null &&
        hasAudio &&
        hasVideo &&
        isHd != null &&
        format != null &&
        extension != null
    ) {
        return Stream(url, format, hasAudio, hasVideo, isHd,extension
        )
    }
    return null
}