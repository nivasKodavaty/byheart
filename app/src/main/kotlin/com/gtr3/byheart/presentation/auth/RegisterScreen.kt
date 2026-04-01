package com.gtr3.byheart.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gtr3.byheart.presentation.theme.AppleYellow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun RegisterScreen(
    onNavigateToNotes: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state        by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var showPassword by remember { mutableStateOf(false) }
    var visible      by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is AuthEffect.NavigateToNotes -> onNavigateToNotes()
                is AuthEffect.ShowError       -> Unit
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // ── Hero section ─────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                AppleYellow.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedVisibility(
                        visible = visible,
                        enter   = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(
                                        color = AppleYellow,
                                        shape = RoundedCornerShape(20.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✦", fontSize = 32.sp, color = MaterialTheme.colorScheme.onPrimary)
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Create Account",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight    = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Start taking smarter notes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Form ─────────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter   = fadeIn() + slideInVertically(initialOffsetY = { it / 4 })
            ) {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {

                    AuthTextField(
                        value         = state.email,
                        onValueChange = { viewModel.onIntent(AuthIntent.EmailChanged(it)) },
                        label         = "Email",
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction    = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    AuthTextField(
                        value                = state.password,
                        onValueChange        = { viewModel.onIntent(AuthIntent.PasswordChanged(it)) },
                        label                = "Password",
                        visualTransformation = if (showPassword) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        keyboardOptions      = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction    = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.onIntent(AuthIntent.Register)
                            }
                        ),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector        = if (showPassword) Icons.Filled.VisibilityOff
                                                         else Icons.Filled.Visibility,
                                    contentDescription = if (showPassword) "Hide" else "Show",
                                    tint               = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )

                    AnimatedVisibility(visible = state.error != null) {
                        state.error?.let { err ->
                            Column {
                                Spacer(Modifier.height(8.dp))
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        err,
                                        style    = MaterialTheme.typography.bodySmall,
                                        color    = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick  = { viewModel.onIntent(AuthIntent.Register) },
                        enabled  = !state.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape  = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor   = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color       = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                "Create Account",
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            "Already have an account?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onNavigateToLogin) {
                            Text(
                                "Sign In",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}
