package com.mpt.masterpasswordtrainer.ui.screens.addentry

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SafetyCheck
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.mpt.masterpasswordtrainer.data.repository.PasswordRepository
import com.mpt.masterpasswordtrainer.ui.navigation.Routes
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEntryScreen(
    navController: NavHostController,
    isFromOnboarding: Boolean = false,
    entryId: String? = null,
    viewModel: AddEntryViewModel = viewModel()
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Password as string for TextField (converted to CharArray on change)
    var passwordText by remember { mutableStateOf("") }
    var confirmPasswordText by remember { mutableStateOf("") }

    // Load existing entry if editing
    LaunchedEffect(entryId) {
        if (!entryId.isNullOrEmpty()) {
            viewModel.loadEntry(entryId)
        }
    }

    // POST_NOTIFICATIONS permission request for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result not needed — gracefully degrades */ }

    // Request notification permission on first entry creation
    LaunchedEffect(Unit) {
        if (!viewModel.isEditMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val repository = PasswordRepository(context)
            if (repository.getAllEntries().isEmpty()) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Staggered entrance animation
    var visibleSections by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        for (i in 1..5) {
            delay(80L)
            visibleSections = i
        }
    }

    val icons = listOf(
        "Shield" to Icons.Filled.Shield,
        "Key" to Icons.Filled.Key,
        "Lock" to Icons.Filled.Lock,
        "Fingerprint" to Icons.Filled.Fingerprint,
        "Safe" to Icons.Filled.SafetyCheck,
        "Cloud" to Icons.Filled.Cloud,
        "Globe" to Icons.Filled.Public,
        "Star" to Icons.Filled.Star,
    )

    val colors = listOf(
        "Red" to 0xFFE53935L,
        "Blue" to 0xFF1E88E5L,
        "Green" to 0xFF43A047L,
        "Purple" to 0xFF8E24AA,
        "Orange" to 0xFFFB8C00L,
        "Teal" to 0xFF00897BL,
        "Pink" to 0xFFD81B60L,
        "Indigo" to 0xFF3949ABL,
    )

    val reminderOptions = listOf(1, 3, 5, 7, 14, 30)

    // Filtered autocomplete suggestions
    val filteredSuggestions = if (viewModel.serviceName.length >= 1) {
        viewModel.serviceSuggestions.filter {
            it.contains(viewModel.serviceName, ignoreCase = true) &&
                !it.equals(viewModel.serviceName, ignoreCase = true)
        }
    } else {
        emptyList()
    }

    val screenTitle = if (viewModel.isEditMode) "Edit Entry" else "Add Entry"
    val saveButtonText = if (viewModel.isEditMode) "Update" else "Save"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General error
            viewModel.errors["general"]?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // --- Section 1: Service ---
            AnimatedSection(visible = visibleSections >= 1, delayMillis = 0) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Service",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Service name with autocomplete
                    Column {
                        OutlinedTextField(
                            value = viewModel.serviceName,
                            onValueChange = { viewModel.updateServiceName(it) },
                            label = { Text("Service name") },
                            placeholder = { Text("e.g. Bitwarden, 1Password") },
                            singleLine = true,
                            isError = viewModel.errors.containsKey("serviceName"),
                            supportingText = viewModel.errors["serviceName"]?.let { { Text(it) } },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Autocomplete chips
                        if (filteredSuggestions.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                filteredSuggestions.forEach { suggestion ->
                                    SuggestionChip(
                                        text = suggestion,
                                        onClick = { viewModel.updateServiceName(suggestion) }
                                    )
                                }
                            }
                        }
                    }

                    // Icon picker
                    Text("Icon", style = MaterialTheme.typography.bodyMedium)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(icons.size) { index ->
                            val (name, icon) = icons[index]
                            IconOption(
                                icon = icon,
                                contentDescription = name,
                                selected = viewModel.serviceIcon == name,
                                accentColor = Color(viewModel.serviceColor),
                                onClick = { viewModel.updateServiceIcon(name) }
                            )
                        }
                    }

                    // Color picker
                    Text("Color", style = MaterialTheme.typography.bodyMedium)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(colors.size) { index ->
                            val (name, colorValue) = colors[index]
                            ColorOption(
                                color = Color(colorValue),
                                contentDescription = name,
                                selected = viewModel.serviceColor == colorValue,
                                onClick = { viewModel.updateServiceColor(colorValue) }
                            )
                        }
                    }
                }
            }

            // --- Section 2: Credentials ---
            AnimatedSection(visible = visibleSections >= 2, delayMillis = 80) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Credentials",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = viewModel.email,
                            onValueChange = { viewModel.updateEmail(it) },
                            label = { Text("Email / Username") },
                            singleLine = true,
                            enabled = viewModel.emailEnabled,
                            isError = viewModel.errors.containsKey("email"),
                            supportingText = viewModel.errors["email"]?.let { { Text(it) } },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Checkbox(
                            checked = viewModel.emailEnabled,
                            onCheckedChange = { viewModel.updateEmailEnabled(it) }
                        )
                    }

                    OutlinedTextField(
                        value = passwordText,
                        onValueChange = { newValue ->
                            passwordText = newValue
                            viewModel.updatePassword(newValue.toCharArray())
                        },
                        label = { Text("Master password") },
                        singleLine = true,
                        isError = viewModel.errors.containsKey("password"),
                        supportingText = viewModel.errors["password"]?.let { { Text(it) } },
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Filled.VisibilityOff
                                    else Icons.Filled.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password"
                                        else "Show password"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPasswordText,
                        onValueChange = { newValue ->
                            confirmPasswordText = newValue
                            viewModel.updateConfirmPassword(newValue.toCharArray())
                        },
                        label = { Text("Confirm password") },
                        singleLine = true,
                        isError = viewModel.errors.containsKey("confirmPassword"),
                        supportingText = viewModel.errors["confirmPassword"]?.let { { Text(it) } }
                            ?: if (confirmPasswordText.isNotEmpty() && passwordText == confirmPasswordText) {
                                { Text("Passwords match", color = MaterialTheme.colorScheme.primary) }
                            } else null,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    if (confirmPasswordVisible) Icons.Filled.VisibilityOff
                                    else Icons.Filled.Visibility,
                                    contentDescription = if (confirmPasswordVisible) "Hide password"
                                        else "Show password"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (viewModel.isEditMode) {
                        Text(
                            text = "Leave password blank to keep current password",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Password hint (optional)
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Password hint (optional)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = viewModel.passwordHint,
                        onValueChange = { viewModel.updatePasswordHint(it) },
                        placeholder = { Text("e.g. childhood pet + graduation year") },
                        singleLine = true,
                        supportingText = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "This hint will be shown after 3 failed attempts. Never write your actual password here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                if (viewModel.passwordHint.isNotEmpty()) {
                                    Text(
                                        text = "${viewModel.passwordHint.length}/100",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // --- Section 3: Reminder ---
            AnimatedSection(visible = visibleSections >= 3, delayMillis = 160) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Reminder interval (days)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        reminderOptions.forEachIndexed { index, days ->
                            SegmentedButton(
                                selected = viewModel.reminderDays == days,
                                onClick = { viewModel.updateReminderDays(days) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = reminderOptions.size
                                )
                            ) {
                                Text("$days")
                            }
                        }
                    }

                    Text(
                        text = if (viewModel.reminderDays == 1) "You'll be reminded to verify every day"
                               else "You'll be reminded to verify every ${viewModel.reminderDays} days",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // --- Section 4: Save button ---
            AnimatedSection(visible = visibleSections >= 4, delayMillis = 240) {
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.save(isFromOnboarding) {
                            if (isFromOnboarding) {
                                navController.navigate(Routes.DASHBOARD) {
                                    popUpTo(0) { inclusive = true }
                                }
                            } else {
                                navController.popBackStack()
                            }
                        }
                    },
                    enabled = !viewModel.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Securing...")
                    } else {
                        Text(saveButtonText, style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun AnimatedSection(
    visible: Boolean,
    delayMillis: Int,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300, delayMillis = delayMillis)) +
                slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    initialOffsetY = { it / 4 }
                )
    ) {
        content()
    }
}

@Composable
private fun IconOption(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val bgColor = if (selected) accentColor.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surfaceVariant
    val iconColor = if (selected) accentColor
        else MaterialTheme.colorScheme.onSurfaceVariant
    val borderMod = if (selected) Modifier.border(2.dp, accentColor, CircleShape)
        else Modifier

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(borderMod)
            .background(bgColor, CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
                this.contentDescription = "$contentDescription icon${if (selected) ", selected" else ""}"
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun ColorOption(
    color: Color,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderMod = if (selected)
        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
    else Modifier

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .then(borderMod)
            .background(color, CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
                this.contentDescription = "$contentDescription color${if (selected) ", selected" else ""}"
            },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(MaterialTheme.colorScheme.onSurface, CircleShape)
            )
        }
    }
}

@Composable
private fun SuggestionChip(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
