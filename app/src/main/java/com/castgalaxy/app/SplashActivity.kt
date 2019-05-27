package com.castgalaxy.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.castgalaxy.app.ui.Introduction
import com.castgalaxy.app.ui.login.ActivationActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (GalaxyCastApplication.getPreferenceHelper().intro) {
            val intent = Intent(this, ActivationActivity::class.java)
            startActivity(intent)
            finish()
        }else{
            val intent = Intent(this, Introduction::class.java)
            startActivity(intent)
            finish()
        }

    }

}