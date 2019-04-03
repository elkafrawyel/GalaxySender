package com.galaxycast.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.galaxycast.app.ui.login.ActivationActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(this, ActivationActivity::class.java)
        startActivity(intent)
        finish()

    }

}