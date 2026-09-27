package org.anmoljeevan.office.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.anmoljeevan.office.core.Text
import org.anmoljeevan.office.core.admin.RegistrationRules
import org.anmoljeevan.office.core.admin.Registration
import org.anmoljeevan.office.core.admin.Statuses
import org.anmoljeevan.office.ui.components.AjmCard
import org.anmoljeevan.office.ui.components.EyebrowText
import org.anmoljeevan.office.ui.components.HeroCard
import org.anmoljeevan.office.ui.components.Notice
import org.anmoljeevan.office.ui.components.NoticeKind
import org.anmoljeevan.office.ui.components.PillOption
import org.anmoljeevan.office.ui.components.PillSwitcher
import org.anmoljeevan.office.ui.components.SoftButton
import org.anmoljeevan.office.ui.components.bouncyClick
import org.anmoljeevan.office.ui.components.staggeredEntrance
import org.anmoljeevan.office.ui.theme.AjmTheme
import org.anmoljeevan.office.ui.theme.Brand
import org.anmoljeevan.office.util.Launch

/** One registration, grown out of its card (a shared-element container transform). */
@Composable
fun RegistrationDetail(
    r: Registration,
    store: AdminStore,
    shared: SharedTransitionScope,
    anim: AnimatedVisibilityScope,
) {
    val context = LocalContext.current
    val c = AjmTheme.colors
    val style = sourceStyle(r.source)
    val hasId = r.id.isNotEmpty()
    val dup = RegistrationRules.dupCount(r, store.dupCounts)
    val key = store.keyFor(r)
    BackHandler { store.detailKey = null }

    with(shared) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .sharedBounds(
                    rememberSharedContentState("card-$key"),
                    animatedVisibilityScope = anim,
                    resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                ),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(store.snackbar) },
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = { store.detailKey = null }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") }
                    },
                    actions = {
                        if (hasId && r.isActive) {
                            IconButton(onClick = { store.askArchive(listOf(r)) }) { Icon(Icons.Rounded.Archive, "Move to history") }
                        }
                        if (hasId) {
                            IconButton(onClick = { store.askDelete(listOf(r)) }) { Icon(Icons.Rounded.Delete, "Delete") }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
        ) { padding ->
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item("header") {
                    HeroCard(
                        Modifier.fillMaxWidth(),
                        brush = Brush.linearGradient(listOf(lerp(style.color, Brand.Navy, 0.62f), lerp(style.color, Brand.Navy, 0.18f))),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .sharedElement(rememberSharedContentState("avatar-$key"), anim)
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(Text.initials(r.name), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    r.name.ifBlank { "(no name)" },
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                    modifier = Modifier.sharedBounds(rememberSharedContentState("name-$key"), anim),
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    Modifier
                                        .clip(RoundedCornerShape(100))
                                        .background(Color.White.copy(alpha = 0.16f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(style.icon, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(r.source.ifBlank { "Other" }, color = Color.White, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Registered " + Text.dateTime(r.timestamp).ifEmpty { "—" },
                            color = c.onHeroMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                if (dup > 1) {
                    item("dup") {
                        Notice("This phone number appears $dup times in the list — check before messaging, so nobody gets it twice.", Modifier.staggeredEntrance(0))
                    }
                }
                if (!r.isActive) {
                    item("archived") {
                        Notice(
                            "In History under “${r.archive}”.",
                            Modifier.staggeredEntrance(0),
                            kind = NoticeKind.INFO,
                            action = { SoftButton("Restore", { store.askRestore(listOf(r)) }, icon = Icons.Rounded.Restore, compact = true) },
                        )
                    }
                }

                item("actions") {
                    Row(Modifier.fillMaxWidth().staggeredEntrance(1), horizontalArrangement = Arrangement.SpaceEvenly) {
                        QuickAction("WhatsApp", Icons.AutoMirrored.Rounded.Chat, c.success, enabled = r.phone.isNotBlank()) { Launch.whatsApp(context, r.phone) }
                        QuickAction("Call", Icons.Rounded.Call, Brand.BrightBlue, enabled = r.phone.isNotBlank()) { Launch.dial(context, r.phone) }
                        QuickAction("Email", Icons.Rounded.Email, Brand.Indigo, enabled = r.email.isNotBlank()) { Launch.email(context, r.email) }
                        QuickAction("Copy", Icons.Rounded.ContentCopy, Brand.Slate, enabled = r.phone.isNotBlank()) { Launch.copy(context, "Phone", r.phone) }
                    }
                }

                item("status") {
                    AjmCard(Modifier.fillMaxWidth().staggeredEntrance(2)) {
                        EyebrowText("Status")
                        Spacer(Modifier.height(10.dp))
                        val selected = Statuses.ALL.indexOf(r.statusOrPending).coerceAtLeast(0)
                        PillSwitcher(
                            options = Statuses.ALL.map { PillOption(it) },
                            selected = selected,
                            onSelect = { i -> store.setStatus(r, Statuses.ALL[i]) },
                            brushFor = { i -> SolidColor(statusColor(Statuses.ALL[i])) },
                        )
                        if (!hasId) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "This row has no ID — change it directly in the sheet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                item("info") {
                    AjmCard(Modifier.fillMaxWidth().staggeredEntrance(3), contentPadding = PaddingValues(vertical = 8.dp)) {
                        InfoRow(Icons.Rounded.Phone, "Phone", r.phone, onClick = if (r.phone.isNotBlank()) ({ Launch.whatsApp(context, r.phone) }) else null)
                        InfoRow(Icons.Rounded.Email, "Email", r.email, onClick = if (r.email.isNotBlank()) ({ Launch.email(context, r.email) }) else null)
                        InfoRow(Icons.AutoMirrored.Rounded.Notes, "Details", r.details)
                        InfoRow(Icons.Rounded.Event, "Registered", Text.dateTime(r.timestamp))
                        if (hasId) InfoRow(Icons.Rounded.Badge, "Registration ID", r.id, small = true)
                    }
                }

                if (hasId) {
                    item("danger") {
                        Row(Modifier.fillMaxWidth().staggeredEntrance(4), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (r.isActive) {
                                SoftButton("Move to history", { store.askArchive(listOf(r)) }, Modifier.weight(1f), icon = Icons.Rounded.Archive)
                            }
                            SoftButton("Delete", { store.askDelete(listOf(r)) }, Modifier.weight(1f), color = c.danger, icon = Icons.Rounded.Delete)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, color: Color, enabled: Boolean, onClick: () -> Unit) {
    val c = AjmTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(76.dp)) {
        Box(
            Modifier
                .size(56.dp)
                .bouncyClick(enabled = enabled, shape = CircleShape, onClick = onClick)
                .background(if (enabled) c.soft(color) else c.chip),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, label, tint = if (enabled) c.ink(color) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String, onClick: (() -> Unit)? = null, small: Boolean = false) {
    val row = Modifier
        .fillMaxWidth()
        .then(if (onClick != null) Modifier.bouncyClick(onClick = onClick) else Modifier)
        .padding(horizontal = 18.dp, vertical = 12.dp)
    Row(row, verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp).padding(top = 2.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value.ifBlank { "—" },
                style = if (small) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge,
                color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
