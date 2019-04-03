package com.galaxycast.app.entity

import com.squareup.moshi.Json

data class VideoResponse(
    @Json(name = "status")
    val status: Boolean?,
    @Json(name = "message")
    val message: String?,
    @Json(name = "description")
    val description: String?,
    @Json(name = "uploader")
    val uploader: String?,
    @Json(name = "url")
    val url: String?,
    @Json(name = "id")
    val id: String?,
    @Json(name = "is_playlist")
    val isPlaylist: Boolean?,
    @Json(name = "site")
    val site: String?,
    @Json(name = "title")
    val title: String?,
    @Json(name = "like_count")
    val likeCount: Int?,
    @Json(name = "dislike_count")
    val dislikeCount: Int?,
    @Json(name = "view_count")
    val viewCount: Int?,
    @Json(name = "duration")
    val duration: Int?,
    @Json(name = "upload_date")
    val uploadDate: String?,
    @Json(name = "tags")
    val tags: List<String?>?,
    @Json(name = "uploader_url")
    val uploaderUrl: String?,
    @Json(name = "thumbnail")
    val thumbnail: String?,
    @Json(name = "streams")
    val streams: List<StreamResponse?>?
)

data class Video(
    val uploader: String,
    val url: String,
    val id: String,
    val title: String,
    val thumbnail: String,
    val streams: List<StreamResponse>
)

fun VideoResponse.toVideo(): Video? {
    if (uploader != null &&
        url != null &&
        id != null &&
        title != null &&
        thumbnail != null &&
        streams != null
    ) {
        return Video(uploader, url, id, title, thumbnail, streams.filterNotNull())
    }
    return null
}

data class StreamResponse(
    @Json(name = "url")
    val url: String?,
    @Json(name = "format")
    val format: String?,
    @Json(name = "format_note")
    val formatNote: String?,
    @Json(name = "extension")
    val extension: String?,
    @Json(name = "video_codec")
    val videoCodec: String?,
    @Json(name = "audio_codec")
    val audioCodec: String?,
    @Json(name = "height")
    val height: Int?,
    @Json(name = "width")
    val width: Int?,
    @Json(name = "fps")
    val fps: Int?,
    @Json(name = "fmt_id")
    val fmtId: String?,
    @Json(name = "filesize")
    val filesize: Int?,
    @Json(name = "filesize_pretty")
    val filesizePretty: String?,
    @Json(name = "has_audio")
    val hasAudio: Boolean?,
    @Json(name = "has_video")
    val hasVideo: Boolean?,
    @Json(name = "is_hd")
    val isHd: Boolean?
)

data class Stream(
    val url: String?,
    val format: String?,
    val hasAudio: Boolean?,
    val hasVideo: Boolean?,
    val isHd: Boolean?
)


fun StreamResponse.toStream(): Stream? {
    if (url != null &&
        hasVideo != null &&
        hasAudio != null
    ) {
        return Stream(url, format, hasAudio, hasVideo, isHd)
    }
    return null
}