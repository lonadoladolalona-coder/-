package org.anmoljeevan.office.ui.media

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import org.anmoljeevan.office.core.media.MediaApi
import org.anmoljeevan.office.core.media.MediaLogin
import org.anmoljeevan.office.data.Settings

// Placeholder while the media screens are being written.
class MediaStore(val api: MediaApi, val login: MediaLogin, val settings: Settings, val scope: CoroutineScope)

@Composable
fun MediaRoot(store: MediaStore, onLogout: () -> Unit) {
    Text("Media — script v${store.login.version}")
}
