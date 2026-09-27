package org.anmoljeevan.office.data

import android.content.Context

/**
 * The few things kept on the phone. The admin and media keys are deliberately NOT here — like
 * the web pages, the app keeps them in memory only and asks again after it is closed.
 */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("ajm-office", Context.MODE_PRIVATE)

    /** The Apps Script Web App URL (not a secret — it is the same one the public site uses). */
    var webAppUrl: String
        get() = prefs.getString(KEY_URL, "").orEmpty()
        set(v) = prefs.edit().putString(KEY_URL, v).apply()

    /** Which side the login screen opens on: "admin" or "media". */
    var lastPortal: String
        get() = prefs.getString(KEY_PORTAL, "admin").orEmpty()
        set(v) = prefs.edit().putString(KEY_PORTAL, v).apply()

    /** The media office "How to use" strip was dismissed on this phone. */
    var mediaHowtoHidden: Boolean
        get() = prefs.getBoolean(KEY_HOWTO, false)
        set(v) = prefs.edit().putBoolean(KEY_HOWTO, v).apply()

    private companion object {
        const val KEY_URL = "webAppUrl"
        const val KEY_PORTAL = "lastPortal"
        const val KEY_HOWTO = "mediaHowtoHidden"
    }
}
