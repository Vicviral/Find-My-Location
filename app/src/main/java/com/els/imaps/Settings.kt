package com.els.imaps

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.els.imaps.data.ThemeData

class Settings : AppCompatActivity() {

    private var switch: Switch? = null
    private lateinit var saveData: ThemeData
    private var backPressedTime: Long = 0
    private var backPressedToast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        saveData = ThemeData(this)
        if (saveData.loadDarkModeState() == true) {
            setTheme(R.style.DarkTheme)
        }else
            setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        //Switch to dark || light mode
        switch = findViewById<View>(R.id.switch_theme) as  Switch?
        if (saveData.loadDarkModeState() == true) {
            switch?.isChecked = true
        }

        switch!!.setOnCheckedChangeListener{_, isChecked ->
            if (isChecked) {
                saveData.setDarkModeState(true)
                restartApp()
            }else {
                saveData.setDarkModeState(false)
                restartApp()
            }
        }
    }

    private fun restartApp() {
        startActivity(Intent(applicationContext, Settings::class.java))
        overridePendingTransition(R.anim.exit_in, R.anim.exit_out)
        finish()
    }

    override fun onBackPressed() {
        startActivity(Intent(this, MapsActivity::class.java))
        overridePendingTransition(R.anim.exit_in, R.anim.exit_out)
        finish()
    }

}