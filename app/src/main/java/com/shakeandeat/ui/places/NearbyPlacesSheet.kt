package com.shakeandeat.ui.places

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shakeandeat.data.LOCATION_PERMISSIONS
import com.shakeandeat.data.NearbyPlace
import com.shakeandeat.data.hasLocationPermission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyPlacesSheet(
    existingPlaceIds: Set<String>,
    onAdd: (NearbyPlace) -> Unit,
    onDismiss: () -> Unit,
    viewModel: NearbyPlacesViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.any { it }) viewModel.load(existingPlaceIds) else viewModel.denied()
    }

    LaunchedEffect(Unit) {
        if (state == NearbyState.Idle) {
            if (hasLocationPermission(context)) viewModel.load(existingPlaceIds)
            else launcher.launch(LOCATION_PERMISSIONS)
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.reset()
            onDismiss()
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Nearby places", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            when (val s = state) {
                NearbyState.Idle, NearbyState.Loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                is NearbyState.Loaded -> if (s.places.isEmpty()) {
                    Text(
                        "Nothing new nearby. You can still type a place in the Add a place box.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                        items(s.places, key = { it.placeId }) { place ->
                            ListItem(
                                headlineContent = { Text(place.name, fontWeight = FontWeight.Bold) },
                                supportingContent = {
                                    Text(
                                        listOfNotNull("%.1f mi".format(place.distanceMi), place.address)
                                            .joinToString(" · "),
                                        maxLines = 1,
                                    )
                                },
                                trailingContent = {
                                    TextButton(
                                        onClick = {
                                            onAdd(place)
                                            viewModel.added(place)
                                        },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.tertiary,
                                        ),
                                    ) { Text("Add", fontWeight = FontWeight.Bold) }
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.background,
                                ),
                            )
                        }
                    }
                }

                is NearbyState.Error -> Column {
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                    TextButton(
                        onClick = { viewModel.load(existingPlaceIds) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.tertiary,
                        ),
                    ) { Text("Try again") }
                }

                NearbyState.Denied -> Text(
                    "Location is off, so we can't suggest places near you. " +
                        "You can still type a place in the Add a place box.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
