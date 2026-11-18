package com.shakeandeat.ui.pick

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shakeandeat.data.PlaceOption
import java.util.Date
import kotlin.math.max
import kotlin.math.roundToInt

private const val WALK_MPH = 3.0

@Composable
fun TodaysPickRoute(
    onBack: () -> Unit,
    onOpenChat: () -> Unit,
    viewModel: TodaysPickViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.reopened) {
        if (state.reopened) {
            onBack()
            viewModel.onReopenedHandled()
        }
    }
    TodaysPickScreen(
        state = state,
        onBack = onBack,
        onOpenChat = onOpenChat,
        onShakeAgain = viewModel::shakeAgain,
    )
}

@Composable
fun TodaysPickScreen(
    state: TodaysPickUiState,
    onBack: () -> Unit,
    onOpenChat: () -> Unit,
    onShakeAgain: () -> Unit,
) {
    val group = state.group
    val winner = state.winner

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            TextButton(
                onClick = onBack,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
            ) { Text("‹ Back to group") }

            when {
                !state.loaded -> Box(Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                group == null || !group.isDecided || winner == null -> Box(Modifier.fillMaxSize()) {
                    Text(
                        text = state.error ?: "No pick yet. Go back, vote, and pick.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                else -> PickBody(
                    winner = winner,
                    memberCount = group.memberUids.size,
                    decidedAtMillis = group.decidedAtMillis,
                    state = state,
                    onOpenChat = onOpenChat,
                    onShakeAgain = onShakeAgain,
                )
            }
        }
    }
}

@Composable
private fun PickBody(
    winner: PlaceOption,
    memberCount: Int,
    decidedAtMillis: Long?,
    state: TodaysPickUiState,
    onOpenChat: () -> Unit,
    onShakeAgain: () -> Unit,
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = winner.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = winner.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            decidedAtMillis?.let {
                Text(
                    text = "Picked · ${DateFormat.getTimeFormat(context).format(Date(it))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(24.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                winner.distanceMi?.let { mi ->
                    val minutes = max(1, (mi / WALK_MPH * 60).roundToInt())
                    InfoRow("Walk", "$minutes min · ${"%.1f".format(mi)} mi")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                }
                InfoRow("Votes", "${winner.votes} of $memberCount")
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                InfoRow("Hours", hoursText(state.hours))
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { openDirections(context, winner.name, winner.placeId) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                ) { Text("Directions", fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    onClick = onOpenChat,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                ) { Text("Group chat", fontWeight = FontWeight.Bold) }
            }

            state.error?.let { message ->
                Spacer(Modifier.height(12.dp))
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Button(
            onClick = onShakeAgain,
            enabled = !state.busy,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp)
                .height(52.dp),
        ) {
            if (state.busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Shake again", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

private fun hoursText(hours: HoursState): String = when (hours) {
    HoursState.Loading -> "Loading…"
    HoursState.Unknown -> "Not available"
    is HoursState.Known -> {
        val today = hours.hours.today
        when {
            hours.hours.openNow == false -> "Closed now"
            today == null -> if (hours.hours.openNow == true) "Open now" else "Not available"
            today.contains("–") -> "Until " + today.substringAfterLast("–").trim()
            else -> today
        }
    }
}

private fun openDirections(context: Context, name: String, placeId: String?) {
    val url = buildString {
        append("https://www.google.com/maps/dir/?api=1&travelmode=walking")
        append("&destination=").append(Uri.encode(name))
        placeId?.let { append("&destination_place_id=").append(it) }
    }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
    }
}
