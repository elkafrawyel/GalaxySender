package com.galaxycast.app.utily

import com.google.android.gms.cast.framework.SessionProvider
import android.content.Context
import com.galaxycast.app.R
import com.galaxycast.app.ui.expandedcontrols.ExpandedControlsActivity
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.NotificationOptions


class CastOptionsProvider : OptionsProvider {

    override fun getCastOptions(context: Context): CastOptions {

        val notificationOptions = NotificationOptions.Builder()
            .setTargetActivityClassName(ExpandedControlsActivity::class.java.getName())
            .build()

        val mediaOptions = CastMediaOptions.Builder()
            .setNotificationOptions(notificationOptions)
            .setExpandedControllerActivityClassName(ExpandedControlsActivity::class.java.getName())
            .build()

        return CastOptions.Builder()
            .setReceiverApplicationId(context.getString(R.string.app_id))
            .setCastMediaOptions(mediaOptions)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
        return null
    }
}
