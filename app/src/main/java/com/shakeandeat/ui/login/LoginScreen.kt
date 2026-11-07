package com.shakeandeat.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shakeandeat.R

@Composable
fun LoginRoute(viewModel: LoginViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LoginScreen(
        state = state,
        onSignIn = viewModel::signIn,
        onRegister = viewModel::register,
        onResetPassword = viewModel::resetPassword,
        onEdit = viewModel::clearMessages,
    )
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    onSignIn: (email: String, password: String) -> Unit,
    onRegister: (name: String, email: String, password: String) -> Unit,
    onResetPassword: (email: String) -> Unit,
    onEdit: () -> Unit,
) {
    // Saveable, so typed text survives rotation and the app being backgrounded and killed.
    var registering by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val submit = {
        focusManager.clearFocus()
        if (registering) onRegister(name, email, password) else onSignIn(email, password)
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.tertiary,
        focusedLabelColor = MaterialTheme.colorScheme.tertiary,
        cursorColor = MaterialTheme.colorScheme.tertiary,
    )

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_fries),
                    contentDescription = "Shake & Eat logo",
                    modifier = Modifier.size(112.dp),
                )
                Text(
                    text = "Shake & Eat",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (registering) "Create your account" else "Sign in to see your lunch groups",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))

                if (registering) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; onEdit() },
                        label = { Text("Name") },
                        singleLine = true,
                        enabled = !state.loading,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; onEdit() },
                    label = { Text("Email") },
                    singleLine = true,
                    enabled = !state.loading,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; onEdit() },
                    label = { Text("Password") },
                    singleLine = true,
                    enabled = !state.loading,
                    visualTransformation =
                        if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(
                            onClick = { showPassword = !showPassword },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.tertiary,
                            ),
                        ) {
                            Text(if (showPassword) "Hide" else "Show")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )

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
                state.info?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }

                Button(
                    onClick = submit,
                    enabled = !state.loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(
                            text = if (registering) "Create account" else "Sign in",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                TextButton(
                    onClick = { registering = !registering; onEdit() },
                    enabled = !state.loading,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.tertiary,
                    ),
                ) {
                    Text(if (registering) "Have an account? Sign in" else "New here? Create account")
                }
                if (!registering) {
                    TextButton(
                        onClick = { onResetPassword(email) },
                        enabled = !state.loading,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.tertiary,
                        ),
                    ) {
                        Text("Forgot password?")
                    }
                }
            }
        }
    }
}
