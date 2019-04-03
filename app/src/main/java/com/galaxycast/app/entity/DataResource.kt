package com.galaxycast.app.entity

sealed class DataResource<out T : Any> {

    data class Success<out T : Any>(val data: T) : DataResource<T>()
    data class Error(val message: String) : DataResource<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Error -> "Error[message=$message]"
        }
    }
}