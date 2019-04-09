package com.castgalaxy.app.ui.player

import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.castgalaxy.app.GalaxyCastApplication
import com.castgalaxy.app.entity.DataResource
import com.castgalaxy.app.entity.Video
import com.castgalaxy.app.useCase.GetVideoUseCase
import kotlinx.coroutines.*

class PlayerViewModel() : ViewModel() {
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var videoJob: Job? = null

    private val videoMutableLiveData = MutableLiveData<VideoState>()
    val videoLiveData: LiveData<VideoState> = videoMutableLiveData

    private val videoUseCase = GetVideoUseCase(GalaxyCastApplication.retrofitService())

    fun getVideo(id_or_url: String, type: PlayerActivity.VideoType) {
        launchGetVideoJob(id_or_url, type)
    }

    private fun launchGetVideoJob(id_or_url: String, type: PlayerActivity.VideoType) {
        if (videoJob?.isActive == true) {
            return
        } else if (!isDeviceOnline()) {
            videoMutableLiveData.value = VideoState.NoConnection
        } else {
            videoJob = scope.launch {
                withContext(Dispatchers.Main) {
                    videoMutableLiveData.value = VideoState.Loading
                }
                val result = videoUseCase.getVideo(id_or_url,type)
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