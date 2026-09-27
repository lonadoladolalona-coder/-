package org.anmoljeevan.office.ui.gate

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.anmoljeevan.office.AppViewModel
import org.anmoljeevan.office.core.WebAppUrl
import org.anmoljeevan.office.ui.components.AjmCard
import org.anmoljeevan.office.ui.components.AuroraBackground
import org.anmoljeevan.office.ui.components.BrandMark
import org.anmoljeevan.office.ui.components.GradientButton
import org.anmoljeevan.office.ui.components.PillOption
import org.anmoljeevan.office.ui.components.PillSwitcher
import org.anmoljeevan.office.ui.components.SheetHeader
import org.anmoljeevan.office.ui.components.Shaker
import org.anmoljeevan.office.ui.components.bouncyClick
import org.anmoljeevan.office.ui.components.shake
import org.anmoljeevan.office.ui.theme.AjmTheme
import org.anmoljeevan.office.ui.theme.Motion

@Composable
fun GateScreen(vm: AppViewModel) {
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    var portal by rememberSaveable { mutableIntStateOf(if (vm.settings.lastPortal == "media") 1 else 0) }
    var key by remember { mutableStateOf("") } // never saved, not even across rotation
    var showKey by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastError by remember { mutableStateOf("") }
    var urlSheet by remember { mutableStateOf(false) }
    val shaker = remember { Shaker() }
    val needsUrl = vm.webAppUrl.isBlank()
    val isAdmin = portal == 0

    fun fail(message: String) {
        error = message
        lastError = message
        scope.launch { shaker.shake() }
    }

    fun submit() {
        if (busy) return
        if (needsUrl) {
            urlSheet = true
            return
        }
        if (key.isBlank()) {
            fail(if (isAdmin) "Enter the admin key." else "Enter the media key.")
            return
        }
        focus.clearFocus()
        busy = true
        error = null
        scope.launch {
            val problem = if (isAdmin) vm.loginAdmin(key.trim()) else vm.loginMedia(key.trim())
            busy = false
            if (problem != null) fail(problem)
        }
    }

    val appear = remember { MutableTransitionState(false).apply { targetState = true } }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AnimatedVisibility(
                visibleState = appear,
                enter = fadeIn(tween(500)) + scaleIn(spring(dampingRatio = 0.55f, stiffness = 220f), initialScale = 0.6f),
            ) { BrandMark(size = 88.dp) }
            Spacer(Modifier.height(16.dp))
            AnimatedVisibility(
                visibleState = appear,
                enter = fadeIn(tween(500, delayMillis = 120)) + slideInVertically(tween(600, delayMillis = 120, easing = Motion.EmphasizedDecelerate)) { it / 2 },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Anmol Jeevan Ministries", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Office · registrations & media",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(26.dp))
            AnimatedVisibility(
                visibleState = appear,
                enter = fadeIn(tween(600, delayMillis = 220)) + slideInVertically(tween(700, delayMillis = 220, easing = Motion.EmphasizedDecelerate)) { it / 4 },
            ) {
                AjmCard(
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp)
                        .shake(shaker),
                    color = AjmTheme.colors.card.copy(alpha = 0.96f),
                    elevation = 18.dp,
                    shape = RoundedCornerShape(30.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                ) {
                    PillSwitcher(
                        options = listOf(PillOption("Registrations", Icons.Rounded.Groups), PillOption("Media Office", Icons.Rounded.VideoLibrary)),
                        selected = portal,
                        onSelect = {
                            portal = it
                            error = null
                        },
                        height = 50.dp,
                    )
                    Spacer(Modifier.height(18.dp))
                    AnimatedContent(
                        targetState = portal,
                        transitionSpec = {
                            val fwd = targetState > initialState
                            (slideInHorizontally(tween(320, easing = Motion.Emphasized)) { if (fwd) it / 3 else -it / 3 } + fadeIn(tween(260))) togetherWith
                                (slideOutHorizontally(tween(320, easing = Motion.Emphasized)) { if (fwd) -it / 3 else it / 3 } + fadeOut(tween(160)))
                        },
                        label = "portalText",
                    ) { p ->
                        Column(Modifier.fillMaxWidth()) {
                            Text(if (p == 0) "Admin" else "Media Office", style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (p == 0) {
                                    "Registrations from Women's Meet, event bookings and speaking invitations — plus Zoom-link sends and history."
                                } else {
                                    "The team's shared content log, weekly plan and content stock."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    OutlinedTextField(
                        value = key,
                        onValueChange = {
                            key = it
                            error = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (isAdmin) "Admin key" else "Media key") },
                        leadingIcon = { Icon(Icons.Rounded.Key, null) },
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                AnimatedContent(showKey, label = "eye") { shown ->
                                    Icon(if (shown) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, if (shown) "Hide key" else "Show key")
                                }
                            }
                        },
                        singleLine = true,
                        isError = error != null,
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { submit() }),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = AjmTheme.colors.subtle,
                            unfocusedContainerColor = AjmTheme.colors.subtle,
                        ),
                    )
                    AnimatedVisibility(visible = error != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        Text(
                            error ?: lastError,
                            color = AjmTheme.colors.danger,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 10.dp, start = 4.dp, end = 4.dp),
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    GradientButton(
                        text = if (needsUrl) "Connect the Google Sheet" else "Enter",
                        onClick = { submit() },
                        loading = busy,
                        icon = Icons.AutoMirrored.Rounded.ArrowForward,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    ServerRow(vm.webAppUrl, onClick = { urlSheet = true })
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "The key is never saved on this phone — you enter it each time you open the app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 420.dp),
            )
        }
    }

    if (urlSheet) {
        ServerSheet(
            current = vm.webAppUrl,
            onSave = {
                vm.saveWebAppUrl(it)
                urlSheet = false
            },
            onDismiss = { urlSheet = false },
        )
    }
}

@Composable
private fun ServerRow(url: String, onClick: () -> Unit) {
    val c = AjmTheme.colors
    val ok = url.isNotBlank()
    Row(
        Modifier
            .fillMaxWidth()
            .bouncyClick(shape = RoundedCornerShape(16.dp), onClick = onClick)
            .background(c.subtle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(c.soft(if (ok) c.success else c.warning)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (ok) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff, null, tint = c.ink(if (ok) c.success else c.warning), modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Google Sheet connection", style = MaterialTheme.typography.labelLarge)
            Text(
                if (ok) shortUrl(url) else "Not set up yet — paste the Web App URL",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(if (ok) "Change" else "Set up", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

private fun shortUrl(url: String): String {
    val s = url.removePrefix("https://")
    val host = s.substringBefore('/')
    val tail = s.takeLast(12)
    return if (s.length <= host.length + 16) s else "$host/…$tail"
}

@Composable
private fun ServerSheet(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var url by remember { mutableStateOf(current) }
    var touched by remember { mutableStateOf(false) }
    val problem = WebAppUrl.problem(url)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 18.dp),
        ) {
            SheetHeader("Connect to your Google Sheet", "One time, on this phone")
            Column(Modifier.padding(horizontal = 22.dp)) {
                Text(
                    "Paste the Apps Script Web App URL — the same one as REG_LOG_URL in js/reglog.js on the website. " +
                        "In the Apps Script editor: Deploy → Manage deployments → copy the Web app URL (it ends in /exec).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        touched = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Web App URL") },
                    placeholder = { Text("https://script.google.com/macros/s/…/exec") },
                    singleLine = true,
                    isError = touched && problem != null,
                    supportingText = {
                        val msg = when {
                            touched && problem != null -> problem
                            url.isNotBlank() && !WebAppUrl.looksLikeAppsScript(url) -> "This doesn't look like an Apps Script /exec URL — it will be used anyway."
                            else -> null
                        }
                        if (msg != null) Text(msg)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (problem == null) onSave(url) else { touched = true } }),
                    shape = RoundedCornerShape(16.dp),
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.weight(1f))
                    GradientButton(
                        "Save",
                        onClick = { if (problem == null) onSave(url) else { touched = true } },
                        enabled = url.isNotBlank(),
                        height = 48.dp,
                    )
                }
            }
        }
    }
}
