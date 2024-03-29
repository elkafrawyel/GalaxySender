package com.castgalaxy.app.ui.search

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.castgalaxy.app.GalaxyCastApplication
import com.castgalaxy.app.entity.YoutubeVideoResponse
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
    val searchLiveData: LiveData<SearchState> = searchMutableLiveData

    fun search(query: String) {
        cashedQuery = query
        nextPageToken = null
        launchSearch(false)
    }

    fun stopSearch() {
        if (searchJob?.isActive == true) {
            searchJob?.cancel()
        }
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
                if (!nextPage) withContext(Dispatchers.Main) {
                    searchMutableLiveData.value = SearchState.Loading
                }

                if (GalaxyCastApplication.getPreferenceHelper().family) {
                    val result = GalaxyCastApplication.retrofitService()
                        .searchFamily(
                            cashedQuery,
                            nextPageToken,
                            GalaxyCastApplication.getPreferenceHelper().active_code,
                            "on"
                        )
                        .await()

                    if (result.status == "success") {
                        nextPageToken = result.nextpagetoken
                        withContext(Dispatchers.Main) {
                            if (nextPage) {
                                searchMutableLiveData.value =
                                    SearchState.NextPage(result.data)
                            } else {
                                searchMutableLiveData.value =
                                    SearchState.Success(result.data)
                            }
                        }
                    }
                } else {
                    val result = GalaxyCastApplication.retrofitService()
                        .search(cashedQuery, nextPageToken, GalaxyCastApplication.getPreferenceHelper().active_code)
                        .await()

                    if (result.status == "success") {
                        nextPageToken = result.nextpagetoken
                        withContext(Dispatchers.Main) {
                            if (nextPage) {
                                searchMutableLiveData.value =
                                    SearchState.NextPage(result.data)
                            } else {
                                searchMutableLiveData.value =
                                    SearchState.Success(result.data)
                            }
                        }
                    }
                }

            }
        }
    }

    private fun isDeviceOnline(): Boolean {
        val connMgr =
            GalaxyCastApplication.instance.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connMgr.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }

    sealed class SearchState {
        object Loading : SearchState()
        data class Success(val data: List<YoutubeVideoResponse?>?) : SearchState()
        data class NextPage(val data: List<YoutubeVideoResponse?>?) : SearchState()
        data class Ended(val data: List<YoutubeVideoResponse?>?) : SearchState()
        sealed class Error : SearchState() {
            object Unknown : Error()
            object Offline : Error()
        }
    }

}