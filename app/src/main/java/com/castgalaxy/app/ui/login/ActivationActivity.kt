package com.castgalaxy.app.ui.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.Toast
import com.castgalaxy.app.R


class ActivationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rotate = RotateAnimation(
            30f,
            360f,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )
        rotate.duration = 2500
        logo.startAnimation(rotate)

        activationMbtn.setOnClickListener {
//            if (activeCode.text.toString().isNotEmpty() && activeCode.text.toString().equals("hmaserv123")) {
                val intent = Intent(this, CheckActivity::class.java)
                startActivity(intent)
                finish()
//            }else{
//                Toast.makeText(this,"Invalid Code",Toast.LENGTH_LONG).show()
//            }
        }
    }
}