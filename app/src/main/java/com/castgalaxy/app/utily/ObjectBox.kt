package com.castgalaxy.app.utily

import android.content.Context
import com.castgalaxy.app.entity.MyObjectBox
import io.objectbox.BoxStore

class ObjectBox {

    companion object {
        lateinit var boxStore: BoxStore
            private set

        fun init(context: Context){
            boxStore = MyObjectBox.builder()
                .androidContext(context.applicationContext)
                .build();
        }
    }
}