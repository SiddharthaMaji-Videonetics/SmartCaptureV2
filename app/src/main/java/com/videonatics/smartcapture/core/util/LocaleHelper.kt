package com.videonatics.smartcapture.core.util

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {
    fun updateLocale(context: Context, languageCode: String): Context {
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)
        
        val configuration = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
        }
        configuration.setLayoutDirection(locale)
        
        val localizedContext = context.createConfigurationContext(configuration)
        
        return object : ContextWrapper(context) {
            override fun getResources() = localizedContext.resources
            override fun getAssets() = localizedContext.assets
            override fun getSystemService(name: String): Any? {
                if (Context.LAYOUT_INFLATER_SERVICE == name) {
                    return super.getSystemService(name)
                }
                return localizedContext.getSystemService(name)
            }
        }
    }
}
