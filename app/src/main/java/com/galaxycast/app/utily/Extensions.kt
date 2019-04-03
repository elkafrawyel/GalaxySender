package com.galaxycast.app.utily

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import com.galaxycast.app.ui.login.REQUEST_GOOGLE_PLAY_SERVICES
import com.galaxycast.app.ui.search.OnQuerySubmit
import com.google.android.gms.common.GoogleApiAvailability

fun SearchView.setOnQuerySubmitListener(listener: (query: String) -> Unit) {
    this.setOnQueryTextListener(object : OnQuerySubmit() {
        override fun onQuerySubmit(query: String) {
            listener.invoke(query)
        }
    })
}

/**
 * Display an error dialog showing that Google Play Services is missing
 * or out of date.
 * @param connectionStatusCode code describing the presence (or lack of)
 * Google Play Services on this device.
 */
fun AppCompatActivity.showGooglePlayServicesAvailabilityErrorDialog(
    connectionStatusCode: Int
) {
    val apiAvailability = GoogleApiAvailability.getInstance()
    val dialog = apiAvailability.getErrorDialog(
        this,
        connectionStatusCode,
        REQUEST_GOOGLE_PLAY_SERVICES
    )
    dialog.show()
}