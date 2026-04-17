package com.videonatics.smartcapture.core

import android.content.Context
import androidx.core.content.edit
import com.videonatics.smartcapture.ui.theme.ThemeMode

class Pref(context: Context) {

    companion object {
        private const val THEME_MODE = "theme_mode"
        private const val MODEL_URL = "model_url"
    }

    private val sharedPreferences = context.getSharedPreferences(
        "${context.packageName}.preferences", Context.MODE_PRIVATE
    )



    var themeModePref: String?
        get() = sharedPreferences.getString(THEME_MODE, ThemeMode.LIGHT.toString())
        set(value) = sharedPreferences.edit { putString(THEME_MODE, value) }




}