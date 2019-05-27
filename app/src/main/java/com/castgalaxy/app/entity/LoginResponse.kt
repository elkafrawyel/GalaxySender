package com.castgalaxy.app.entity
import com.squareup.moshi.Json

data class LoginResponse(
    @field:Json(name = "status")
    val status: String?,
    @field:Json(name = "Code")
    val code: String?,
    @field:Json(name = "Expiration date")
    val expirationDate: String?,
    @field:Json(name = "License")
    val license: String?,
    @field:Json(name = "update")
    val update: Boolean?,
    @field:Json(name = "version")
    val version: String?,
    @field:Json(name = "url")
    val url: String?
)