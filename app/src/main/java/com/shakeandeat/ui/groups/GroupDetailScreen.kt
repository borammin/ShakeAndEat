package com.shakeandeat.ui.groups

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shakeandeat.data.Group
import com.shakeandeat.data.NearbyPlace
import com.shakeandeat.data.SearchResult
import com.shakeandeat.data.PlaceOption
import com.shakeandeat.sensors.ShakeEffect
import com.shakeandeat.sensors.rememberShakeAvailable
import com.shakeandeat.ui.places.NearbyPlacesSheet

@Composable
fun GroupDetailRoute(
    onBack: () -> Unit,
    onOpenPick: () -> Unit = {},
    onOpenChat: () -> Unit = {},
    viewModel: GroupDetailViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.openPick) {
        if (state.openPick) {
            onOpenPick()
            viewModel.onPickOpened()
        }
    }
    GroupDetailScreen(
        state = state,
        onBack = onBack,
        onAddOption = viewModel::addOption,
        onToggleVote = viewModel::toggleVote,
        onRemoveOption = viewModel::removeOption,
        onPickWinner = viewModel::pickWinner,
        onReopen = viewModel::reopenVoting,
        onEdit = viewModel::clearError,
        onAddPlace = viewModel::addPlace,
        onAddSearchedPlace = viewModel::addSearchedPlace,
        onOpenPick = onOpenPick,
        onOpenChat = onOpenChat,
    )
}

@Composable
fun GroupDetailScreen(
    state: GroupDetailUiState,
    onBack: () -> Unit,
    onAddOption: (String) -> Unit,
    onToggleVote: (PlaceOption) -> Unit,
    onRemoveOption: (PlaceOption) -> Unit = {},
    onPickWinner: () -> Unit,
    onReopen: () -> Unit,
    onEdit: () -> Unit,
    onAddPlace: (NearbyPlace) -> Unit = {},
    onAddSearchedPlace: (SearchResult) -> Unit = {},
    onOpenPick: () -> Unit = {},
    onOpenChat: () -> Unit = {},
) {
    var newPlace by rememberSaveable { mutableStateOf("") }
    var showNearby by rememberSaveable { mutableStateOf(false) }
    var pendingRemoveId by rememberSaveable { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val group = state.group

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.tertiary,
        focusedLabelColor = MaterialTheme.colorScheme.tertiary,
        cursorColor = MaterialTheme.colorScheme.tertiary,
    )

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onBack,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
                ) { Text("‹ Back to groups") }
                Spacer(Modifier.weight(1f))
                if (group != null) {
                    Button(
                        onClick = onOpenChat,
                        shape = RoundedCornerShape(50),
                    ) { Text("Chat", fontWeight = FontWeight.Bold) }
                }
            }

            when {
                !state.loaded -> Box(Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                group == null -> Box(Modifier.fillMaxSize()) {
                    Text(
                        text = state.error ?: "This group no longer exists.",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                else -> GroupBody(
                    group = group,
                    state = state,
                    newPlace = newPlace,
                    onNewPlaceChange = { newPlace = it.take(80); onEdit() },
                    onAdd = {
                        focusManager.clearFocus()
                        onAddOption(newPlace)
                        if (newPlace.isNotBlank()) newPlace = ""
                    },
                    fieldColors = fieldColors,
                    onToggleVote = onToggleVote,
                    onAskRemove = { pendingRemoveId = it.id },
                    onPickWinner = onPickWinner,
                    onReopen = onReopen,
                    onOpenPick = onOpenPick,
                    onShowNearby = { showNearby = true },
                )
            }
        }
    }

    val pendingRemove = state.options.firstOrNull { it.id == pendingRemoveId }
    if (pendingRemove != null) {
        AlertDialog(
            onDismissRequest = { pendingRemoveId = null },
            title = { Text("Remove ${pendingRemove.name}?") },
            text = { Text("It will disappear for everyone in the group, along with its votes.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveOption(pendingRemove)
                        pendingRemoveId = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Remove", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingRemoveId = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
                ) { Text("Cancel") }
            },
        )
    }

    if (showNearby && group != null) {
        NearbyPlacesSheet(
            existingPlaceIds = state.options.mapNotNull { it.placeId }.toSet(),
            onAdd = onAddPlace,
            onAddSearched = onAddSearchedPlace,
            onDismiss = { showNearby = false },
        )
    }
}

@Composable
private fun ColumnScope.GroupBody(
    group: Group,
    state: GroupDetailUiState,
    newPlace: String,
    onNewPlaceChange: (String) -> Unit,
    onAdd: () -> Unit,
    fieldColors: androidx.compose.material3.TextFieldColors,
    onToggleVote: (PlaceOption) -> Unit,
    onAskRemove: (PlaceOption) -> Unit,
    onPickWinner: () -> Unit,
    onReopen: () -> Unit,
    onOpenPick: () -> Unit,
    onShowNearby: () -> Unit,
) {
    val context = LocalContext.current
    val members = if (group.memberUids.size == 1) "1 member" else "${group.memberUids.size} members"

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(group.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            text = "$members · " + if (group.isDecided) "decided" else "voting open",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Invite code", style = MaterialTheme.typography.labelMedium)
                Text(group.id, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Join my Shake & Eat lunch group \"${group.name}\" with code ${group.id}")
                    }
                    context.startActivity(Intent.createChooser(send, "Share invite code"))
                },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
            ) { Text("Share") }
        }
    }

    if (group.isDecided) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Today's pick", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
                Text(
                    text = state.winner?.name ?: "(that place was removed)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                state.winner?.let {
                    Text(
                        text = if (it.votes == 1) "1 vote" else "${it.votes} votes",
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onOpenPick,
                        enabled = state.winner != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                    ) { Text("See today's pick") }
                    OutlinedButton(onClick = onReopen, enabled = !state.busy) { Text("Reopen voting") }
                }
            }
        }
    }

    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        if (state.options.isEmpty()) {
            Text(
                text = "No places yet. Add the first one below.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.options, key = { it.id }) { option ->
                    OptionRow(
                        option = option,
                        iVoted = state.myUid != null && state.myUid in option.voteUids,
                        isWinner = group.isDecided && option.id == group.winnerOptionId,
                        votingOpen = !group.isDecided,
                        onToggleVote = { onToggleVote(option) },
                        canRemove = state.myUid != null &&
                            (option.addedByUid == state.myUid || group.ownerUid == state.myUid),
                        onRemove = { onAskRemove(option) },
                    )
                }
            }
        }
    }

    state.error?.let { message ->
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }

    if (!group.isDecided) {
        OutlinedButton(
            onClick = onShowNearby,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("+ Find a place") }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = newPlace,
                onValueChange = onNewPlaceChange,
                label = { Text("Add a place") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onAdd() }),
                colors = fieldColors,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = onAdd, modifier = Modifier.height(56.dp)) { Text("Add") }
        }
        Button(
            onClick = onPickWinner,
            enabled = state.options.isNotEmpty() && !state.busy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Pick today's winner", fontWeight = FontWeight.Bold) }
        // Shake is an extra: it runs the same action as the button above.
        ShakeEffect(enabled = state.options.size >= 2 && !state.busy, onShake = onPickWinner)
        if (rememberShakeAvailable()) {
            Text(
                "or shake your phone",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun OptionRow(
    option: PlaceOption,
    iVoted: Boolean,
    isWinner: Boolean,
    votingOpen: Boolean,
    onToggleVote: () -> Unit,
    canRemove: Boolean,
    onRemove: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isWinner) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = (if (isWinner) "★ " else "") + option.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = (if (option.votes == 1) "1 vote" else "${option.votes} votes") +
                        (option.distanceMi?.let { " · %.1f mi".format(it) } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (votingOpen && canRemove) {
                TextButton(
                    onClick = onRemove,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.semantics { contentDescription = "Remove ${option.name}" },
                ) { Text("✕") }
            }
            if (votingOpen) {
                if (iVoted) {
                    FilledTonalButton(onClick = onToggleVote) { Text("Voted ✓") }
                } else {
                    OutlinedButton(onClick = onToggleVote) { Text("Vote") }
                }
            }
        }
    }
}
