package com.els.imaps.data

import android.content.Context
import android.content.SharedPreferences

class ThemeData(context: Context) {
    private var sharedPreferences: SharedPreferences = context.getSharedPreferences("file", Context.MODE_PRIVATE)

    //Save the dark mode state : TRUE || FALSE
    fun setDarkModeState(state: Boolean?){
        val editor = sharedPreferences.edit()
        editor.putBoolean("Dark", state!!)
        editor.apply()
    }

    //Loads the dark mode state
    fun loadDarkModeState(): Boolean? {
        val state = sharedPreferences.getBoolean("Dark", false)
        return (state)
    }
}