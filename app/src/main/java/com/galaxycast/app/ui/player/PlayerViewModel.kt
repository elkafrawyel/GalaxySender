package com.galaxycast.app.ui.player

import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.galaxycast.app.GalaxyCastApplication
import com.galaxycast.app.entity.DataResource
import com.galaxycast.app.entity.Video
import com.galaxycast.app.useCase.GetVideoUseCase
import kotlinx.coroutines.*

class PlayerViewModel() : ViewModel() {
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var videoJob: Job? = null

    private val videoMutableLiveData = MutableLiveData<VideoState>()
    val videoLiveData: LiveData<VideoState> = videoMutableLiveData

    val videoUseCase = GetVideoUseCase(GalaxyCastApplication.retrofitService())

    fun getVideo(videoId: String) {
        launchGetVideoJob(videoId)
    }

    private fun launchGetVideoJob(videoId: String) {
        if (videoJob?.isActive == true) {
            return
        } else if (!isDeviceOnline()) {
            videoMutableLiveData.value = VideoState.NoConnection
        } else {
            videoJob = scope.launch {
                withContext(Dispatchers.Main) {
                    videoMutableLiveData.value = VideoState.Loading
                }
                val result = videoUseCase.getVideo(videoId)
                withContext(Dispatchers.Main) {
                    when (result) {

                        is DataResource.Success -> videoMutableLiveData.value = VideoState.Success(result.data)
                        is DataResource.Error -> videoMutableLiveData.value = VideoState.Error(result.message)
                    }
                }
            }
        }
    }

    sealed class VideoState {
        object Loading : VideoState()
        data class Success(val data: Video) : VideoState()
        data class Error(val message: String) : VideoState()
        object NoConnection : VideoState()
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
}