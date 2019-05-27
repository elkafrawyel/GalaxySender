package com.castgalaxy.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.castgalaxy.app.GalaxyCastApplication
import com.castgalaxy.app.R
import com.castgalaxy.app.ui.login.ActivationActivity
import com.github.paolorotolo.appintro.AppIntro
import com.github.paolorotolo.appintro.AppIntroFragment
import java.util.*

class Introduction : AppIntro() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        changeLanguage(GalaxyCastApplication.getPreferenceHelper().language)

        addSlide(
            AppIntroFragment.newInstance(
                resources.getString(R.string.intro_header_1),
                resources.getString(R.string.intro_body_1),
                R.drawable.ic_cast_white_large, Color.parseColor("#de2c66")
            )
        )

        addSlide(
            AppIntroFragment.newInstance(
                resources.getString(R.string.intro_header_2),
                resources.getString(R.string.intro_body_2),
                R.drawable.ic_videocam_white_24dp, Color.parseColor("#de2c66")
            )
        )

        addSlide(
            AppIntroFragment.newInstance(
                resources.getString(R.string.intro_header_3),
                resources.getString(R.string.intro_body_3),
                R.drawable.ic_location_on_white_24dp, Color.parseColor("#de2c66")
            )
        )

        setDepthAnimation()


        // setFadeAnimation();
        //        setFadeAnimation();
        //        setZoomAnimation();
        //        setFlowAnimation();
        //        setSlideOverAnimation();
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        // Do something when users tap on Skip button.
        GalaxyCastApplication.getPreferenceHelper().intro = true
        val intent = Intent(this, ActivationActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        // Do something when users tap on Done button.
        GalaxyCastApplication.getPreferenceHelper().intro = true
        val intent = Intent(this, ActivationActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onSlideChanged(oldFragment: Fragment?, newFragment: Fragment?) {
        super.onSlideChanged(oldFragment, newFragment)
        // Do something when the slide changes.

    }

    fun Context.changeLanguage(lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = this.resources.configuration
        config.setLocale(locale)
        this.createConfigurationContext(config)
        this.resources.updateConfiguration(config, this.resources.displayMetrics)
    }
}
