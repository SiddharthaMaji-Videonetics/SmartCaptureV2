package com.videonatics.smartcapture.core

import android.content.Context
import androidx.core.content.edit
import com.videonatics.smartcapture.ui.theme.ThemeMode

class Pref(context: Context) {

    companion object {
        private const val THEME_MODE = "theme_mode"
        private const val BASE_URL = "base_url"
        private const val MODEL_URL = "model_url"
        private const val IS_LOGGED_IN = "is_logged_in"
        private const val ACCESS_TOKEN = "access_token"
        private const val USER_ID = "user_id"
        private const val USER_NAME = "user_name"
        private const val USER_EMAIL = "user_email"
        private const val USER_TYPE = "user_type"
        private const val SELECTED_LANGUAGE_CODE = "selected_language_code"
        private const val LAST_EMAIL = "last_email"
        private const val LAST_PASSWORD = "last_password"
        private const val IS_TEST_USER = "is_test_user"
    }

    private val sharedPreferences = context.getSharedPreferences(
        "${context.packageName}.preferences", Context.MODE_PRIVATE
    )



    var themeModePref: String?
        get() = sharedPreferences.getString(THEME_MODE, ThemeMode.LIGHT.toString())
        set(value) = sharedPreferences.edit { putString(THEME_MODE, value) }

    var baseUrlPref: String
        get() = sharedPreferences.getString(BASE_URL, "") ?: ""
        set(value) = sharedPreferences.edit { putString(BASE_URL, value) }

    var modelUrlPref: String
        get() = sharedPreferences.getString(MODEL_URL, "") ?: ""
        set(value) = sharedPreferences.edit { putString(MODEL_URL, value) }

    var isLoggedInPref: Boolean
        get() = sharedPreferences.getBoolean(IS_LOGGED_IN, false)
        set(value) = sharedPreferences.edit { putBoolean(IS_LOGGED_IN, value) }

    var accessToken: String?
        get() = sharedPreferences.getString(ACCESS_TOKEN, null)
        set(value) = sharedPreferences.edit { putString(ACCESS_TOKEN, value) }

    var userId: String
        get() = sharedPreferences.getString(USER_ID, "") ?: ""
        set(value) = sharedPreferences.edit { putString(USER_ID, value) }

    var userName: String
        get() = sharedPreferences.getString(USER_NAME, "") ?: ""
        set(value) = sharedPreferences.edit { putString(USER_NAME, value) }

    var userEmail: String
        get() = sharedPreferences.getString(USER_EMAIL, "") ?: ""
        set(value) = sharedPreferences.edit { putString(USER_EMAIL, value) }

    var userType: String
        get() = sharedPreferences.getString(USER_TYPE, "") ?: ""
        set(value) = sharedPreferences.edit { putString(USER_TYPE, value) }

    var selectedLanguageCode: String
        get() = sharedPreferences.getString(SELECTED_LANGUAGE_CODE, "en") ?: "en"
        set(value) = sharedPreferences.edit { putString(SELECTED_LANGUAGE_CODE, value) }

    var lastEmail: String
        get() = sharedPreferences.getString(LAST_EMAIL, "") ?: ""
        set(value) = sharedPreferences.edit { putString(LAST_EMAIL, value) }

    var lastPassword: String
        get() = sharedPreferences.getString(LAST_PASSWORD, "") ?: ""
        set(value) = sharedPreferences.edit { putString(LAST_PASSWORD, value) }

    var isTestUser: Boolean
        get() = sharedPreferences.getBoolean(IS_TEST_USER, false)
        set(value) = sharedPreferences.edit { putBoolean(IS_TEST_USER, value) }
}
