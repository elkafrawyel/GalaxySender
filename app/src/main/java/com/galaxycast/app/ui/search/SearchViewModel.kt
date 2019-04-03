package com.galaxycast.app.ui.search

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.galaxycast.app.GalaxyCastApplication
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.model.SearchResult
import kotlinx.coroutines.*

class SearchViewModel : ViewModel() {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var searchJob: Job? = null

    private var cashedQuery: String = ""
    private var nextPageToken: String? = null

    private val searchMutableLiveData = MutableLiveData<SearchState>()
    val searchLiveData : LiveData<SearchState> = searchMutableLiveData

    fun search(query: String) {
        cashedQuery = query
        nextPageToken = null
        launchSearch(false)
    }

    fun nextPage() {
        launchSearch(true)
    }

    private fun launchSearch(nextPage: Boolean) {
        if (searchJob?.isActive == true) {
            return
        } else if (!isDeviceOnline()) {
            searchMutableLiveData.value = SearchState.Error.Offline
        } else {
            searchJob = scope.launch {
                if (!nextPage) withContext(Dispatchers.Main) { searchMutableLiveData.value =
                    SearchState.Loading
                }
                val transport = AndroidHttp.newCompatibleTransport()
                val jsonFactory = JacksonFactory.getDefaultInstance()
                val mService = com.google.api.services.youtube.YouTube.Builder(
                    transport, jsonFactory, GalaxyCastApplication.mCredential
                )
                    .setApplicationName("Galaxy Cast")
                    .build()

                try {
                    val searchResult = mService?.search()
                        ?.list("snippet")
                        ?.setQ(cashedQuery)
                        ?.setType("video")
                        ?.setMaxResults(50)
                        ?.setPageToken(nextPageToken)
                        ?.execute()

                    nextPageToken = searchResult?.nextPageToken
                    val result = ArrayList(searchResult?.items ?: emptyList())
                    withContext(Dispatchers.Main) {
                        if (nextPage) {
                            searchMutableLiveData.value =
                                SearchState.NextPage(result)
                        } else {
                            searchMutableLiveData.value =
                                SearchState.Success(result)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    when (e) {
                        is GooglePlayServicesAvailabilityIOException -> withContext(Dispatchers.Main) {
                            searchMutableLiveData.value =
                                SearchState.Error.GooglePlay(e.connectionStatusCode)
                        }
                        is UserRecoverableAuthIOException -> withContext(Dispatchers.Main) {
                            searchMutableLiveData.value =
                                SearchState.Error.UserRecoverableAuth(e.intent)
                        }
                        else -> withContext(Dispatchers.Main) {
                            searchMutableLiveData.value =
                                SearchState.Error.Unknown
                        }
                    }
                }
            }
        }
    }

    private fun isDeviceOnline(): Boolean {
        val connMgr = GalaxyCastApplication.instance.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connMgr.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }

    sealed class SearchState {
        object Loading : SearchState()
        data class Success(val data: List<SearchResult>) : SearchState()
        data class NextPage(val data: List<SearchResult>) : SearchState()
        data class Ended(val data: List<SearchResult>) : SearchState()
        sealed class Error : SearchState() {
            object Unknown : Error()
            object Offline : Error()
            data class GooglePlay(val connectionStatusCode: Int) : Error()
            data class UserRecoverableAuth(val intent: Intent) : Error()
        }
    }

}