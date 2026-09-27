package org.anmoljeevan.office.ui.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.anmoljeevan.office.core.Text
import org.anmoljeevan.office.core.admin.Registration
import org.anmoljeevan.office.ui.components.AjmCard
import org.anmoljeevan.office.ui.components.Avatar
import org.anmoljeevan.office.ui.components.CelebrationArt
import org.anmoljeevan.office.ui.components.EyebrowText
import org.anmoljeevan.office.ui.components.GradientButton
import org.anmoljeevan.office.ui.components.Notice
import org.anmoljeevan.office.ui.components.PredictiveBackScreen
import org.anmoljeevan.office.ui.components.SoftButton
import org.anmoljeevan.office.ui.components.TintBadge
import org.anmoljeevan.office.ui.components.bouncyClick
import org.anmoljeevan.office.ui.components.softShadow
import org.anmoljeevan.office.ui.theme.AjmTheme
import org.anmoljeevan.office.ui.theme.Brand
import org.anmoljeevan.office.ui.theme.Motion
import org.anmoljeevan.office.util.Launch

private val WhatsAppGreen = Brush.linearGradient(listOf(Color(0xFF128C7E), Color(0xFF25D366)))

@Composable
fun ZoomSendScreen(store: AdminStore, send: ZoomSend) {
    var confirmClose by remember { mutableStateOf(false) }

    fun close() {
        if (send.failed.isNotEmpty()) confirmClose = true else store.closeOverlay()
    }

    PredictiveBackScreen(onBack = {
        if (send.step == ZoomSend.Step.QUEUE) send.stop() else close()
    }) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(store.snackbar) },
            topBar = {
                TopAppBar(
                    title = {
                        AnimatedContent(send.step, label = "zoomTitle") { step ->
                            Text(if (step == ZoomSend.Step.QUEUE) "Sending Zoom link" else "Send Zoom Link")
                        }
                    },
                    navigationIcon = { IconButton(onClick = ::close) { Icon(Icons.Rounded.Close, "Close") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
        ) { padding ->
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                AnimatedContent(
                    targetState = send.step,
                    transitionSpec = {
                        (slideInHorizontally(tween(420, easing = Motion.Emphasized)) { it / 3 } + fadeIn(tween(300))) togetherWith
                            (slideOutHorizontally(tween(420, easing = Motion.Emphasized)) { -it / 3 } + fadeOut(tween(200)))
                    },
                    label = "zoomStep",
                ) { step ->
                    when (step) {
                        ZoomSend.Step.SETUP -> Setup(store, send)
                        ZoomSend.Step.QUEUE -> Queue(send)
                        ZoomSend.Step.DONE -> Done(store, send, onClose = ::close)
                    }
                }
            }
        }
    }

    if (confirmClose) {
        AlertDialog(
            onDismissRequest = { confirmClose = false },
            title = { Text("Close anyway?") },
            text = { Text("Some results could not be saved to history yet.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClose = false
                    store.closeOverlay()
                }) { Text("Close") }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmClose = false
                    send.retryFailed()
                }) { Text("Retry saving") }
            },
        )
    }
}

@Composable
private fun Setup(store: AdminStore, send: ZoomSend) {
    val c = AjmTheme.colors
    var problem by remember { mutableStateOf<String?>(null) }
    val sent = send.sentBefore()
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier
                .weight(1f)
                .imePadding(),
            contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    "Goes to everyone marked Confirmed in the current tab (${send.label}). Opens one WhatsApp chat at a time, pre-filled — you just hit send for each.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when (store.historyState) {
                HistoryState.UNSUPPORTED -> item {
                    Notice("Meeting history isn’t switched on yet — the Google Sheet script needs one update (see ADMIN-SETUP.md). You can still send; this send just won’t be saved.")
                }
                HistoryState.ERROR, HistoryState.UNKNOWN -> item {
                    Notice("Couldn’t reach the meeting history right now, so this send won’t be saved. Close and try again in a moment.")
                }
                HistoryState.OK -> item {
                    AjmCard(Modifier.fillMaxWidth()) {
                        EyebrowText("Meeting name")
                        Text(
                            "This send is saved under it in History",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = send.meetingName,
                            onValueChange = { if (it.length <= 80) send.updateMeetingName(it) },
                            singleLine = true,
                            placeholder = { Text("e.g. Women's Meet — 28 Sep") },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        val names = store.meetings.map { it.name }.filter { it != send.meetingName }.take(8)
                        if (names.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(names) { n -> SuggestionChip(onClick = { send.updateMeetingName(n) }, label = { Text(n, maxLines = 1) }) }
                            }
                        }
                    }
                }
            }
            item {
                AjmCard(Modifier.fillMaxWidth()) {
                    EyebrowText("Message")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = send.message,
                        onValueChange = send::updateMessage,
                        minLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("{name} is replaced with each person's name automatically.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val first = send.pool.firstOrNull()
                    if (first != null) {
                        Spacer(Modifier.height(14.dp))
                        EyebrowText("Preview")
                        Spacer(Modifier.height(8.dp))
                        ChatBubble(send.personalMessage(first))
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Recipients (${send.pool.size})", style = MaterialTheme.typography.titleMedium)
                        if (send.pool.isNotEmpty()) {
                            Text("${send.checked.size} of ${send.pool.size} selected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    TextButton(onClick = send::selectAll, enabled = send.checked.size < send.pool.size) { Text("Select all") }
                    TextButton(onClick = send::selectNone, enabled = send.checked.isNotEmpty()) { Text("Clear") }
                }
            }
            if (send.pool.isEmpty()) {
                item {
                    Notice("No Confirmed registrants in this view. Mark people Confirmed first, or open the All tab.", kind = org.anmoljeevan.office.ui.components.NoticeKind.INFO)
                }
            }
            itemsIndexed(send.pool, key = { i, r -> r.key + i }) { i, r ->
                RecipientRow(r, checked = i in send.checked, sentBefore = sent.contains(r), onToggle = { send.toggle(i) })
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            AnimatedVisibility(problem != null) {
                Text(problem.orEmpty(), color = c.danger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
            }
            GradientButton(
                text = if (send.checked.isEmpty()) "Start sending" else "Start sending (${send.checked.size})",
                onClick = { problem = send.start() },
                enabled = send.pool.isNotEmpty(),
                icon = Icons.AutoMirrored.Rounded.Send,
                brush = WhatsAppGreen,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ChatBubble(text: String) {
    val c = AjmTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (c.isDark) Color(0xFF0B141A) else Color(0xFFEFE7DE))
            .padding(12.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (c.isDark) Color(0xFFE9EDEF) else Color(0xFF111B21),
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 4.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                .background(if (c.isDark) Color(0xFF005C4B) else Color(0xFFD9FDD3))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun RecipientRow(r: Registration, checked: Boolean, sentBefore: Boolean, onToggle: () -> Unit) {
    val c = AjmTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .bouncyClick(shape = RoundedCornerShape(18.dp), onClick = onToggle)
            .background(c.card)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Avatar(Text.initials(r.name), sourceStyle(r.source).color, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(r.name.ifBlank { "(no name)" }, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(r.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        if (sentBefore) {
            TintBadge("✓ sent before", c.success)
            Spacer(Modifier.width(8.dp))
        }
    }
}

/** One person at a time, on a little stack of cards. */
@Composable
private fun Queue(send: ZoomSend) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val total = send.queue.size
    val progress by animateFloatAsState(if (total == 0) 0f else send.index / total.toFloat(), spring(stiffness = 90f), label = "queueProgress")
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            EyebrowText("${(send.index + 1).coerceAtMost(total)} of $total", Modifier.weight(1f))
            Text("✓ ${send.sentCount} sent", style = MaterialTheme.typography.labelLarge, color = AjmTheme.colors.success)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(100)),
            color = Color(0xFF25D366),
            trackColor = AjmTheme.colors.chip,
        )
        Spacer(Modifier.height(28.dp))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            // the next two people peek out from behind the current card
            val left = total - send.index - 1
            if (left >= 2) StackShadow(depth = 2)
            if (left >= 1) StackShadow(depth = 1)
            AnimatedContent(
                targetState = send.index to send.opened,
                transitionSpec = {
                    if (targetState.first != initialState.first) {
                        (fadeIn(tween(320, 120)) + scaleIn(spring(dampingRatio = 0.7f, stiffness = 300f), initialScale = 0.92f)) togetherWith
                            (slideOutHorizontally(tween(320, easing = Motion.EmphasizedAccelerate)) { -it } + fadeOut(tween(260)))
                    } else {
                        fadeIn(tween(220)) togetherWith fadeOut(tween(120))
                    }
                },
                label = "queueCard",
            ) { (i, opened) ->
                val person = send.queue.getOrNull(i)
                if (person != null) {
                    PersonCard(
                        person = person,
                        opened = opened,
                        onOpen = {
                            Launch.whatsApp(context, person.phone, send.personalMessage(person))
                            send.markOpened()
                        },
                        onReopen = { Launch.whatsApp(context, person.phone, send.personalMessage(person)) },
                        onSent = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            send.confirmSent()
                        },
                        onSkip = send::skip,
                        onStop = send::stop,
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        HistoryStatus(send)
    }
}

@Composable
private fun BoxScope.StackShadow(depth: Int) {
    val c = AjmTheme.colors
    Box(
        Modifier
            .matchParentSize()
            .padding(horizontal = (depth * 14).dp)
            .offset(y = (depth * 12).dp)
            .graphicsLayer { alpha = 1f - depth * 0.28f }
            .softShadow(RoundedCornerShape(28.dp), 4.dp, c.shadow.copy(alpha = 0.25f))
            .background(c.card),
    )
}

@Composable
private fun PersonCard(
    person: Registration,
    opened: Boolean,
    onOpen: () -> Unit,
    onReopen: () -> Unit,
    onSent: () -> Unit,
    onSkip: () -> Unit,
    onStop: () -> Unit,
) {
    AjmCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), elevation = 14.dp, contentPadding = PaddingValues(22.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar(Text.initials(person.name), sourceStyle(person.source).color, size = 76.dp)
            Spacer(Modifier.height(14.dp))
            Text(person.name.ifBlank { "(no name)" }, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text(person.phone, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            AnimatedVisibility(opened, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Text(
                    "Did you hit send in WhatsApp?",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
            }
            if (!opened) {
                GradientButton("Open WhatsApp", onClick = onOpen, icon = Icons.Rounded.Chat, brush = WhatsAppGreen, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SoftButton("Skip", onSkip, color = Brand.Slate, icon = Icons.Rounded.SkipNext)
                    SoftButton("Stop", onStop, color = AjmTheme.colors.danger, icon = Icons.Rounded.Stop)
                }
            } else {
                GradientButton("Yes, sent — Next", onClick = onSent, icon = Icons.Rounded.Check, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoftButton("Reopen", onReopen, color = Color(0xFF128C7E), icon = Icons.Rounded.Refresh, compact = true)
                    SoftButton("Skip (not sent)", onSkip, color = Brand.Slate, icon = Icons.Rounded.SkipNext, compact = true)
                    SoftButton("Stop", onStop, color = AjmTheme.colors.danger, icon = Icons.Rounded.Stop, compact = true)
                }
            }
        }
    }
}

@Composable
private fun HistoryStatus(send: ZoomSend) {
    if (send.savingAs.isEmpty() || !send.historyOn) return
    val c = AjmTheme.colors
    val (color, text) = when {
        send.failed.isNotEmpty() -> c.danger to "${send.failed.size} result${if (send.failed.size == 1) "" else "s"} could not be saved to history"
        send.inFlight > 0 -> MaterialTheme.colorScheme.primary to "Saving to history — “${send.savingAs}”…"
        else -> c.success to "Saved to history as “${send.savingAs}”"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.soft(color))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(send.inFlight > 0 && send.failed.isEmpty(), label = "saveIcon") { busy ->
            if (busy) {
                CircularProgressIndicator(Modifier.size(16.dp), color = c.ink(color), strokeWidth = 2.dp)
            } else {
                Icon(if (send.failed.isEmpty()) Icons.Rounded.Check else Icons.Rounded.Close, null, tint = c.ink(color), modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = c.ink(color), modifier = Modifier.weight(1f))
        if (send.failed.isNotEmpty()) SoftButton("Retry", send::retryFailed, color = c.danger, compact = true)
    }
}

@Composable
private fun Done(store: AdminStore, send: ZoomSend, onClose: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CelebrationArt(size = 150.dp)
        Spacer(Modifier.height(20.dp))
        Text("All done", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(8.dp))
        val total = send.queue.size
        Text(
            "Confirmed sent to ${send.sentCount} of $total recipient${if (total == 1) "" else "s"}${if (send.stoppedEarly) " (stopped early)" else ""}.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        HistoryStatus(send)
        Spacer(Modifier.height(24.dp))
        GradientButton("Close", onClick = onClose, modifier = Modifier.fillMaxWidth())
        if (send.historyOn) {
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = {
                store.closeOverlay()
                store.openHistory(0)
            }) {
                Icon(Icons.Rounded.History, null)
                Spacer(Modifier.width(8.dp))
                Text("View history", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}
