package com.shakeandeat.ui.groups

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shakeandeat.data.Group
import com.shakeandeat.ui.theme.NavyLight
import com.shakeandeat.ui.theme.Orange
import com.shakeandeat.ui.theme.OrangeTint

@Composable
fun GroupsRoute(
    email: String?,
    onOpenGroup: (String) -> Unit,
    onSignOut: () -> Unit,
    viewModel: GroupsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GroupsScreen(
        state = state,
        email = email,
        onCreate = viewModel::createGroup,
        onJoin = viewModel::joinGroup,
        onEdit = viewModel::clearError,
        onOpen = onOpenGroup,
        onOpened = viewModel::onOpened,
        onSignOut = onSignOut,
    )
}

@Composable
fun GroupsScreen(
    state: GroupsUiState,
    email: String?,
    onCreate: (String) -> Unit,
    onJoin: (String) -> Unit,
    onEdit: () -> Unit,
    onOpen: (String) -> Unit,
    onOpened: () -> Unit,
    onSignOut: () -> Unit,
) {
    // Saveable, so typed text and the open dialog survive rotation and the app being killed.
    var newName by rememberSaveable { mutableStateOf("") }
    var joinCode by rememberSaveable { mutableStateOf("") }
    var showCreate by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val dark = isSystemInDarkTheme()

    // After creating or joining, open that group once and reset the form.
    LaunchedEffect(state.openGroup) {
        val code = state.openGroup ?: return@LaunchedEffect
        newName = ""
        joinCode = ""
        showCreate = false
        onOpen(code)
        onOpened()
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.tertiary,
        focusedLabelColor = MaterialTheme.colorScheme.tertiary,
        cursorColor = MaterialTheme.colorScheme.tertiary,
    )
    val fieldShape = RoundedCornerShape(12.dp)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            // Plain header on the page background, with Sign out kept on the right.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "My lunch groups",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!email.isNullOrBlank()) {
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                OutlinedButton(onClick = onSignOut) { Text("Sign out") }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                when {
                    !state.loaded -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    state.loadError != null -> Text(
                        text = state.loadError,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center,
                    )
                    state.groups.isEmpty() -> Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("No groups yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Create one below, or join a friend's with their invite code.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        // Groups still voting come first, as they are the ones to act on.
                        val sorted = state.groups.sortedWith(compareBy({ it.isDecided }, { it.name.lowercase() }))
                        items(sorted, key = { it.id }) { group ->
                            GroupCard(group = group, dark = dark, onClick = { onOpen(group.id) })
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!showCreate) {
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
                }

                OutlinedTextField(
                    value = joinCode,
                    onValueChange = {
                        joinCode = it.filter(Char::isLetterOrDigit).uppercase().take(6)
                        onEdit()
                    },
                    placeholder = { Text("Join with code: ______") },
                    singleLine = true,
                    enabled = !state.busy,
                    shape = fieldShape,
                    trailingIcon = {
                        TextButton(
                            onClick = {
                                focusManager.clearFocus()
                                onJoin(joinCode)
                            },
                            enabled = !state.busy && joinCode.length == 6,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
                        ) { Text("Join", fontWeight = FontWeight.Bold) }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        onJoin(joinCode)
                    }),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onEdit()
                        showCreate = true
                    },
                    enabled = !state.busy,
                    shape = fieldShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    if (state.busy && !showCreate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("+ Create group", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = {
                if (!state.busy) {
                    showCreate = false
                    onEdit()
                }
            },
            title = { Text("New lunch group") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it.take(40); onEdit() },
                        label = { Text("Group name") },
                        singleLine = true,
                        enabled = !state.busy,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { onCreate(newName) }),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.error?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onCreate(newName) },
                    enabled = !state.busy,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
                ) {
                    if (state.busy) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Create", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreate = false
                        onEdit()
                    },
                    enabled = !state.busy,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
                ) { Text("Cancel") }
            },
        )
    }
}

/** A group still being voted on is highlighted in orange; a decided one is plain. */
@Composable
private fun GroupCard(group: Group, dark: Boolean, onClick: () -> Unit) {
    val open = !group.isDecided
    val members = if (group.memberUids.size == 1) "1 member" else "${group.memberUids.size} members"
    val status = if (open) "voting open" else "decided"
    val container: Color = when {
        open && dark -> NavyLight
        open -> OrangeTint
        else -> MaterialTheme.colorScheme.surface
    }
    val border = if (open) {
        BorderStroke(1.5.dp, Orange)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = border,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 104.dp)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "$members · $status",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
