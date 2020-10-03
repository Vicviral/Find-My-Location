package com.els.imaps.intro

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.els.imaps.MapsActivity
import com.els.imaps.R
import com.els.imaps.data.ThemeData
import kotlinx.android.synthetic.main.activity_splash_screen.*

class SplashScreen : AppCompatActivity() {

    private lateinit var saveData: ThemeData

    override fun onCreate(savedInstanceState: Bundle?) {

        saveData = ThemeData(this)
        if (saveData.loadDarkModeState() == true) {
            setTheme(R.style.SplashDarkTheme)
        }else
            setTheme(R.style.SplashTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        Handler().postDelayed({
            motto.startAnimation(AnimationUtils.loadAnimation(this, R.anim.zoom_in))

            Handler().postDelayed({
                startActivity(Intent(this, MapsActivity::class.java))
                overridePendingTransition(R.anim.exit_in, R.anim.exit_out)
                finish()
            }, 500)
        }, 2500)
    }
}