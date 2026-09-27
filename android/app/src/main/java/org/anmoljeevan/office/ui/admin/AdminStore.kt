package org.anmoljeevan.office.ui.admin

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import org.anmoljeevan.office.core.admin.AdminApi
import org.anmoljeevan.office.core.admin.RegistrationList

// Placeholder while the admin screens are being written.
class AdminStore(val api: AdminApi, val initial: RegistrationList, val scope: CoroutineScope)

@Composable
fun AdminRoot(store: AdminStore, onLogout: () -> Unit) {
    Text("Admin — ${store.initial.rows.size} registrations")
}
