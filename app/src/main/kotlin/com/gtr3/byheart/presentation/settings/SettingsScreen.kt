@file:OptIn(ExperimentalMaterial3Api::class)

package com.gtr3.byheart.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val SEX_OPTIONS = listOf("Male", "Female", "Non-binary", "Prefer not to say")

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showSexDropdown by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                SettingsEffect.NavigateToLogin -> onLogout()
                SettingsEffect.SaveSuccess     -> onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp).padding(end = 4.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        TextButton(
                            onClick  = { viewModel.onIntent(SettingsIntent.Save) },
                            enabled  = !state.isLoading
                        ) {
                            Text("Save", fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 2.5.dp)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // ── Avatar ────────────────────────────────────────────────────────
            InitialsAvatar(
                name     = state.displayName.ifBlank { state.email },
                modifier = Modifier.size(88.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text  = state.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            // ── Profile fields ────────────────────────────────────────────────
            SettingsSection("Profile") {
                SettingsTextField(
                    label    = "Display Name",
                    value    = state.displayName,
                    onChange = { viewModel.onIntent(SettingsIntent.DisplayNameChanged(it)) },
                    placeholder = "Your name"
                )
                Spacer(Modifier.height(12.dp))

                // Date of Birth picker row
                SettingsClickableField(
                    label      = "Date of Birth",
                    value      = state.dateOfBirth.toDisplayDate(),
                    icon       = Icons.Default.CalendarMonth,
                    placeholder = "Not set",
                    onClick    = { showDatePicker = true }
                )
                Spacer(Modifier.height(12.dp))

                // Sex dropdown
                ExposedDropdownMenuBox(
                    expanded         = showSexDropdown,
                    onExpandedChange = { showSexDropdown = it }
                ) {
                    OutlinedTextField(
                        value            = state.sex.ifBlank { "" },
                        onValueChange    = {},
                        readOnly         = true,
                        label            = { Text("Sex") },
                        placeholder      = { Text("Not set", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        trailingIcon     = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSexDropdown)
                        },
                        modifier         = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape            = RoundedCornerShape(14.dp),
                        colors           = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded         = showSexDropdown,
                        onDismissRequest = { showSexDropdown = false }
                    ) {
                        SEX_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text    = { Text(option) },
                                onClick = {
                                    viewModel.onIntent(SettingsIntent.SexChanged(option))
                                    showSexDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Account section ───────────────────────────────────────────────
            SettingsSection("Account") {
                OutlinedTextField(
                    value         = state.email,
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text("Email") },
                    enabled       = false,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(14.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor      = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        disabledTextColor        = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        disabledLabelColor       = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── Logout ────────────────────────────────────────────────────────
            OutlinedButton(
                onClick = { showLogoutConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border   = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                )
            ) {
                Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Log Out", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    // ── Date Picker dialog ─────────────────────────────────────────────────────
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.dateOfBirth
                .toEpochMillis() ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton    = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        viewModel.onIntent(
                            SettingsIntent.DateOfBirthChanged(
                                date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            )
                        )
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    // ── Logout confirm dialog ──────────────────────────────────────────────────
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title   = { Text("Log out?") },
            text    = { Text("You'll need to sign in again to access your notes.") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    viewModel.onIntent(SettingsIntent.Logout)
                }) {
                    Text("Log out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // ── Error snackbar ─────────────────────────────────────────────────────────
    state.error?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(SettingsIntent.DismissError) },
            title   = { Text("Error") },
            text    = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { viewModel.onIntent(SettingsIntent.DismissError) }) { Text("OK") }
            }
        )
    }
}

// ── Reusable components ────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text     = title.uppercase(),
            style    = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 12.dp, start = 2.dp)
        )
        content()
    }
}

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onChange,
        label         = { Text(label) },
        placeholder   = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
        singleLine    = true,
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(14.dp),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    )
}

@Composable
private fun SettingsClickableField(
    label: String,
    value: String,
    icon: ImageVector,
    placeholder: String,
    onClick: () -> Unit
) {
    OutlinedTextField(
        value         = value.ifBlank { "" },
        onValueChange = {},
        readOnly      = true,
        label         = { Text(label) },
        placeholder   = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
        trailingIcon  = {
            Icon(icon, null, modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        modifier      = Modifier.fillMaxWidth().clickable(onClick = onClick),
        enabled       = false,
        shape         = RoundedCornerShape(14.dp),
        colors        = OutlinedTextFieldDefaults.colors(
            disabledBorderColor  = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            disabledTextColor    = MaterialTheme.colorScheme.onSurface,
            disabledLabelColor   = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
fun InitialsAvatar(name: String, modifier: Modifier = Modifier) {
    val initials = name.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { name.take(1).uppercase().ifBlank { "?" } }

    val avatarColor = remember(name) {
        val hue = ((name.hashCode() % 360 + 360) % 360).toFloat()
        Color.hsl(hue, 0.42f, 0.50f)
    }

    Box(
        modifier         = modifier.clip(CircleShape).background(avatarColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = initials,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun String.toDisplayDate(): String = try {
    LocalDate.parse(this).format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
} catch (_: Exception) { "" }

private fun String.toEpochMillis(): Long? = try {
    LocalDate.parse(this).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
} catch (_: Exception) { null }
