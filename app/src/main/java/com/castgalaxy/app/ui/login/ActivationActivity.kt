package com.castgalaxy.app.ui.login

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.Toast
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.DeviceUtils
import com.blankj.utilcode.util.Utils
import com.castgalaxy.app.GalaxyCastApplication
import com.castgalaxy.app.R
import com.castgalaxy.app.entity.LoginResponse
import com.castgalaxy.app.remote.RetrofitService
import com.castgalaxy.app.ui.search.SearchActivity
import com.crashlytics.android.Crashlytics
import com.google.api.services.youtube.YouTube
import io.fabric.sdk.android.Fabric
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ActivationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        val rotate = RotateAnimation(
//            30f,
//            360f,
//            Animation.RELATIVE_TO_SELF,
//            0.5f,
//            Animation.RELATIVE_TO_SELF,
//            0.5f
//        )
//        rotate.duration = 2500
//        logo.startAnimation(rotate)


        activationMbtn.setOnClickListener {

            if (activeCode.text.toString().isNotEmpty()) {

                val call = GalaxyCastApplication.retrofitService()
                    .login(
                        activeCode.text.toString(),
                        DeviceUtils.getAndroidID(),
                        AppUtils.getAppVersionCode().toString()
                    )

                call.enqueue(object : Callback<LoginResponse?> {
                    override fun onFailure(call: Call<LoginResponse?>, t: Throwable) {

                    }

                    override fun onResponse(call: Call<LoginResponse?>, response: Response<LoginResponse?>) {
                        if (response.isSuccessful) {
                            val result = response.body()!!

                            if (result.status == "active") {
                                if (result.update!!) {
                                    Toast.makeText(this@ActivationActivity, "Update Available", Toast.LENGTH_LONG)
                                        .show()
                                    val openURL = Intent(android.content.Intent.ACTION_VIEW)
                                    openURL.data = Uri.parse(result.url)
                                    startActivity(openURL)
                                } else {
                                    GalaxyCastApplication.getPreferenceHelper().active_code = activeCode.text.toString()
                                    GalaxyCastApplication.getPreferenceHelper().date = result.expirationDate
                                    GalaxyCastApplication.getPreferenceHelper().licence = result.license

                                    val intent = Intent(this@ActivationActivity, SearchActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }

                            } else if (result.status == "expired") {
                                Toast.makeText(this@ActivationActivity, "Expired Code", Toast.LENGTH_LONG).show()
                            } else if (result.status == "failed") {
                                Toast.makeText(this@ActivationActivity, "Invalid Code", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                })

            } else {
                Toast.makeText(this, "Empty Code", Toast.LENGTH_LONG).show()
            }
        }
        if (GalaxyCastApplication.getPreferenceHelper().active_code != null) {
            activeCode.setText(GalaxyCastApplication.getPreferenceHelper().active_code)
            activationMbtn.performClick()
        }

        Fabric.with(this@ActivationActivity, Crashlytics())

    }
}