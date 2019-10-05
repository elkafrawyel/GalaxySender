package com.castgalaxy.app.local

import android.content.Context
import android.preference.PreferenceManager

class PreferencesHelper(context: Context) {

    companion object {
        private const val Intro = "intro"
        private const val CODE = "code"
        private const val ACTIVE_CODE = "active_code"
        private const val IMAGE = "image"
        private const val TYPE = "type"
        private const val ISPLAYING = "is_playing"
        private const val LANGUAGE = "language"
        private const val AUTO_PLAY = "auto_play"
        private const val DATE = "date"
        private const val LICENCE = "licence"
        private const val FAMILY = "family"
    }

    private val preference = PreferenceManager.getDefaultSharedPreferences(context)

    var intro = preference.getBoolean(Intro, false)
        set(value) = preference.edit().putBoolean(Intro, value).apply()

    var code = preference.getString(CODE, null)
        set(value) = preference.edit().putString(CODE, value).apply()

    var date = preference.getString(DATE, null)
        set(value) = preference.edit().putString(DATE, value).apply()

    var licence = preference.getString(LICENCE, null)
        set(value) = preference.edit().putString(LICENCE, value).apply()

    var active_code = preference.getString(ACTIVE_CODE, null)
        set(value) = preference.edit().putString(ACTIVE_CODE, value).apply()

    var image = preference.getString(IMAGE, null)
        set(value) = preference.edit().putString(IMAGE, value).apply()

    var type = preference.getString(TYPE, "single")
        set(value) = preference.edit().putString(TYPE, value).apply()

    var is_playing = preference.getBoolean(ISPLAYING, false)
        set(value) = preference.edit().putBoolean(ISPLAYING, value).apply()

    var autoPlay = preference.getBoolean(AUTO_PLAY, false)
        set(value) = preference.edit().putBoolean(AUTO_PLAY, value).apply()

    var language = preference.getString(LANGUAGE, "en")
        set(value) = preference.edit().putString(LANGUAGE, value).apply()

    var family = preference.getBoolean(FAMILY, false)
        set(value) = preference.edit().putBoolean(FAMILY, value).apply()


    fun clear() {
        val intro = this.intro
        preference.edit().clear().apply()
        this.intro = intro
    }
}