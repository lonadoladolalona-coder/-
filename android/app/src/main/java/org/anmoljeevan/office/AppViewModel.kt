package org.anmoljeevan.office

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.anmoljeevan.office.core.ApiException
import org.anmoljeevan.office.core.ScriptClient
import org.anmoljeevan.office.core.WebAppUrl
import org.anmoljeevan.office.core.admin.AdminApi
import org.anmoljeevan.office.core.media.MediaApi
import org.anmoljeevan.office.data.Settings
import org.anmoljeevan.office.ui.admin.AdminStore
import org.anmoljeevan.office.ui.media.MediaStore

/** Which part of the app is open. */
sealed interface Portal {
    data object Gate : Portal
    class Admin(val store: AdminStore) : Portal
    class Media(val store: MediaStore) : Portal
}

/**
 * Holds the session for as long as the app is open (it survives rotation). The key lives only
 * in memory, inside the logged-in store — nothing secret is written to the phone.
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {
    val settings = Settings(app)

    var webAppUrl by mutableStateOf(settings.webAppUrl.ifBlank { BuildConfig.DEFAULT_WEB_APP_URL })
        private set

    var portal by mutableStateOf<Portal>(Portal.Gate)
        private set

    private var sessionScope: CoroutineScope? = null

    fun saveWebAppUrl(url: String) {
        val clean = WebAppUrl.normalize(url)
        settings.webAppUrl = clean
        webAppUrl = clean
    }

    private fun newSessionScope(): CoroutineScope {
        sessionScope?.cancel()
        return CoroutineScope(viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext[Job])).also { sessionScope = it }
    }

    /** Returns null when it worked, or the message to show. */
    suspend fun loginAdmin(key: String): String? {
        settings.lastPortal = "admin"
        val api = AdminApi(ScriptClient(webAppUrl), key)
        return try {
            val list = api.list()
            portal = Portal.Admin(AdminStore(api, list, newSessionScope()))
            null
        } catch (e: ApiException) {
            messageFor(e, wrongKey = "Wrong admin key.")
        }
    }

    suspend fun loginMedia(key: String): String? {
        settings.lastPortal = "media"
        val api = MediaApi(ScriptClient(webAppUrl), key)
        return try {
            val login = api.login()
            if (!login.supported) {
                return "The Google Sheet script has not been updated to the latest version yet (see ADMIN-SETUP.md)."
            }
            portal = Portal.Media(MediaStore(api, login, settings, newSessionScope()))
            null
        } catch (e: ApiException) {
            messageFor(e, wrongKey = "Wrong key — or the media key has not been set in the Google Sheet script yet (see ADMIN-SETUP.md).")
        }
    }

    fun logout() {
        sessionScope?.cancel()
        sessionScope = null
        portal = Portal.Gate
    }

    private fun messageFor(e: ApiException, wrongKey: String): String = when (e) {
        is ApiException.Unauthorized -> wrongKey
        is ApiException.BadResponse ->
            "The Web App URL answered with a web page instead of data. Check it is the /exec URL of the current deployment, and that the deployment is open to “Anyone”."
        is ApiException.Server -> "The sheet answered: ${e.code}"
        is ApiException.Network ->
            "Could not reach the sheet — Google Apps Script can be slow to respond, especially on the first try. Wait a few seconds and try again."
    }
}
