package com.videonatics.smartcapture.core

import android.content.Context
import androidx.core.content.edit
import com.videonatics.smartcapture.ui.theme.ThemeMode

class Pref(context: Context) {

    companion object {
        private const val THEME_MODE = "theme_mode"
        private const val MODEL_URL = "model_url"
        private const val IS_LOGGED_IN = "is_logged_in"
    }

    private val sharedPreferences = context.getSharedPreferences(
        "${context.packageName}.preferences", Context.MODE_PRIVATE
    )



    var themeModePref: String?
        get() = sharedPreferences.getString(THEME_MODE, ThemeMode.LIGHT.toString())
        set(value) = sharedPreferences.edit { putString(THEME_MODE, value) }

    var modelUrlPref: String
        get() = sharedPreferences.getString(MODEL_URL, "") ?: ""
        set(value) = sharedPreferences.edit { putString(MODEL_URL, value) }

    var isLoggedInPref: Boolean
        get() = sharedPreferences.getBoolean(IS_LOGGED_IN, false)
        set(value) = sharedPreferences.edit { putBoolean(IS_LOGGED_IN, value) }





}