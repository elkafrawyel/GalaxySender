package com.castgalaxy.app.local

import android.content.Context
import android.preference.PreferenceManager

class PreferencesHelper(context: Context) {
    companion object {
        private const val CODE = "code"
        private const val ACTIVE_CODE = "active_code"
        private const val IMAGE = "image"
        private const val TYPE = "type"
        private const val ISPLAYING = "is_playing"
    }

    private val preference = PreferenceManager.getDefaultSharedPreferences(context)

    var code = preference.getString(CODE, null)
        set(value) = preference.edit().putString(CODE, value).apply()

    var active_code = preference.getString(ACTIVE_CODE, null)
        set(value) = preference.edit().putString(ACTIVE_CODE, value).apply()

    var image = preference.getString(IMAGE, null)
        set(value) = preference.edit().putString(IMAGE, value).apply()

    var type = preference.getString(TYPE, "single")
        set(value) = preference.edit().putString(TYPE, value).apply()

    var is_playing = preference.getBoolean(ISPLAYING, false)
        set(value) = preference.edit().putBoolean(ISPLAYING, value).apply()

}