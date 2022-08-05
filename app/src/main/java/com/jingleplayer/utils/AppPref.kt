package com.jingleplayer.utils

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences

class AppPref(context: Context) {
    fun getInt(key: String?): Int {
        return appSharedPref.getInt(key, 0)
    }

    fun setInt(key: String?, value: Int) {
        prefEditor.putInt(key, value).apply()
    }

    fun clearInt(key: String?) {
        setInt(key, 0)
    }

    fun getString(key: String?): String? {
        return appSharedPref.getString(key, "")
    }

    fun setString(key: String?, value: String?) {
        prefEditor.putString(key, value).apply()
    }

    fun getBoolean(key: String): Boolean {
        return appSharedPref.getBoolean(key, false)
    }

    fun setBoolean(key: String?, value: Boolean) {
        prefEditor.putBoolean(key, value).apply()
    }

    fun clearString(key: String?) {
        setString(key, "")
    }

    companion object {
        const val APP_PERMISSION_DATA = "APP_PERMISSION_DATA"
        lateinit var appSharedPref: SharedPreferences
        lateinit var prefEditor: SharedPreferences.Editor
        const val PERMITTED_AUDIO_STORAGE = "PERMITTED_AUDIO_STORAGE"
        const val PERMITTED_VIDEO_STORAGE = "PERMITTED_VIDEO_STORAGE"
    }

    init {
        appSharedPref = context.getSharedPreferences(APP_PERMISSION_DATA, Activity.MODE_PRIVATE)
        prefEditor = appSharedPref.edit()
    }
}
