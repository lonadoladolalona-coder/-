package org.anmoljeevan.office.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.anmoljeevan.office.core.Text
import org.anmoljeevan.office.core.admin.Registration
import org.anmoljeevan.office.core.admin.RegistrationRules
import org.anmoljeevan.office.ui.theme.AjmTheme
import java.time.LocalDate

/** The "are you sure / which event" questions for moving, deleting and restoring. */
@Composable
fun AdminDialogs(store: AdminStore) {
    when (val d = store.dialog) {
        is AdminDialog.Archive -> ArchiveDialog(store, d.rows)
        is AdminDialog.Delete -> DeleteDialog(store, d.rows)
        is AdminDialog.Restore -> RestoreDialog(store, d.rows)
        null -> Unit
    }
}

private fun plural(n: Int, one: String, many: String) = if (n == 1) one else many

@Composable
private fun Progress(store: AdminStore) {
    AnimatedVisibility(visible = store.dialogProgress != null) {
        Column(Modifier.padding(top = 14.dp)) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Text(store.dialogProgress.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ArchiveDialog(store: AdminStore, rows: List<Registration>) {
    val n = rows.size
    val busy = store.dialogProgress != null
    var name by remember(rows) { mutableStateOf(RegistrationRules.defaultArchiveName(rows, LocalDate.now())) }
    val clean = Text.clean(name)
    AlertDialog(
        onDismissRequest = { if (!busy) store.dialog = null },
        icon = { Icon(Icons.Rounded.Archive, null) },
        title = { Text("Move $n to History") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "${plural(n, "This registration leaves", "These registrations leave")} the main list and " +
                        "${plural(n, "is", "are")} kept in History under the event name below. Nothing is deleted, and you can " +
                        "restore ${plural(n, "it", "them")} any time.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 80) name = it },
                    label = { Text("Event name") },
                    singleLine = true,
                    enabled = !busy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                val names = store.archiveNames.filter { it != clean }.take(6)
                if (names.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        names.forEach { s -> SuggestionChip(onClick = { name = s }, label = { Text(s, maxLines = 1) }) }
                    }
                }
                NamesPreview(rows)
                Progress(store)
            }
        },
        confirmButton = {
            Button(onClick = { store.archive(rows, clean) }, enabled = !busy && clean.isNotEmpty()) { Text("Move $n to History") }
        },
        dismissButton = {
            TextButton(onClick = { store.dialog = null }, enabled = !busy) { Text("Cancel") }
        },
    )
}

@Composable
private fun DeleteDialog(store: AdminStore, rows: List<Registration>) {
    val n = rows.size
    val busy = store.dialogProgress != null
    val c = AjmTheme.colors
    AlertDialog(
        onDismissRequest = { if (!busy) store.dialog = null },
        icon = { Icon(Icons.Rounded.DeleteForever, null, tint = c.danger) },
        title = { Text("Delete $n ${plural(n, "registration", "registrations")}?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "This permanently removes ${plural(n, "it", "them")} from the sheet and can’t be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                NamesPreview(rows)
                if (store.archiveSupported && rows.all { it.isActive }) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Not sure? Move to history keeps them safely, out of the main list.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { store.dialog = AdminDialog.Archive(rows) }, enabled = !busy) { Text("Move to history instead") }
                }
                Progress(store)
            }
        },
        confirmButton = {
            Button(
                onClick = { store.delete(rows) },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = c.danger, contentColor = MaterialTheme.colorScheme.onError),
            ) { Text("Delete $n") }
        },
        dismissButton = {
            Row { TextButton(onClick = { store.dialog = null }, enabled = !busy) { Text("Cancel") } }
        },
    )
}

@Composable
private fun RestoreDialog(store: AdminStore, rows: List<Registration>) {
    val n = rows.size
    val busy = store.dialogProgress != null
    AlertDialog(
        onDismissRequest = { if (!busy) store.dialog = null },
        icon = { Icon(Icons.Rounded.Restore, null) },
        title = { Text(if (n == 1) "Restore ${rows[0].name.ifBlank { "this person" }}?" else "Restore $n people?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "${plural(n, "They move", "They all move")} back to the main list, with their status kept.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                NamesPreview(rows)
                Progress(store)
            }
        },
        confirmButton = { Button(onClick = { store.restore(rows) }, enabled = !busy) { Text("Restore") } },
        dismissButton = { TextButton(onClick = { store.dialog = null }, enabled = !busy) { Text("Cancel") } },
    )
}
