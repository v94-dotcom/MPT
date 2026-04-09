package com.mpt.masterpasswordtrainer.ui.screens.settings

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.mpt.masterpasswordtrainer.data.backup.BackupManager
import com.mpt.masterpasswordtrainer.ui.navigation.Routes
import com.mpt.masterpasswordtrainer.ui.screens.settings.SettingsViewModel.Companion.PANIC_WIPE_THRESHOLD_OPTIONS
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = viewModel()
) {
    val appLockEnabled by viewModel.appLockEnabled.collectAsState()
    val defaultReminderDays by viewModel.defaultReminderDays.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val quietHoursStart by viewModel.quietHoursStart.collectAsState()
    val quietHoursEnd by viewModel.quietHoursEnd.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val adaptiveDifficulty by viewModel.adaptiveDifficulty.collectAsState()

    val panicWipeEnabled by viewModel.panicWipeEnabled.collectAsState()
    val panicWipeThreshold by viewModel.panicWipeThreshold.collectAsState()

    val backupBytes by viewModel.backupBytes.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val importResult by viewModel.importResult.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteConfirmText by remember { mutableStateOf("") }
    var showPanicWipeConfirmDialog by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    // Backup & Restore state
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportWarningDialog by remember { mutableStateOf(false) }
    var showImportPasswordDialog by remember { mutableStateOf(false) }
    var importFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    val context = LocalContext.current

    // File save picker for export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            val bytes = backupBytes
            if (bytes != null) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                    Toast.makeText(context, "Backup saved successfully", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    Toast.makeText(context, "Failed to save backup", Toast.LENGTH_SHORT).show()
                }
            }
            viewModel.clearBackupBytes()
        }
    }

    // File open picker for import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null && bytes.isNotEmpty()) {
                    importFileBytes = bytes
                    showImportPasswordDialog = true
                } else {
                    Toast.makeText(context, "Could not read backup file", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(context, "Failed to open backup file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launch file save picker when backup bytes become available
    LaunchedEffect(backupBytes) {
        val bytes = backupBytes
        if (bytes != null) {
            val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            exportLauncher.launch("mpt_backup_$date.mptbackup")
        }
    }

    // Handle import result
    LaunchedEffect(importResult) {
        when (val result = importResult) {
            is BackupManager.ImportResult.Success -> {
                Toast.makeText(context, "Backup restored — ${result.entryCount} entries imported", Toast.LENGTH_LONG).show()
                viewModel.clearImportResult()
            }
            is BackupManager.ImportResult.WrongPassword -> {
                Toast.makeText(context, "Incorrect password or corrupted backup file. Try again.", Toast.LENGTH_LONG).show()
                viewModel.clearImportResult()
            }
            is BackupManager.ImportResult.CorruptedFile -> {
                Toast.makeText(context, "Corrupted or invalid backup file", Toast.LENGTH_LONG).show()
                viewModel.clearImportResult()
            }
            is BackupManager.ImportResult.Error -> {
                Toast.makeText(context, "Restore failed. Please try again.", Toast.LENGTH_LONG).show()
                viewModel.clearImportResult()
            }
            null -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // --- App Lock Section ---
            SectionHeader(icon = { Icon(Icons.Filled.Fingerprint, contentDescription = null) }, title = "Security")
            SettingsRow(
                title = "App Lock",
                subtitle = "Require biometric or PIN to open MPT",
                trailing = {
                    Switch(
                        checked = appLockEnabled,
                        onCheckedChange = { viewModel.setAppLockEnabled(it) }
                    )
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsRow(
                title = "Panic wipe",
                subtitle = "Auto-delete all data after consecutive failures",
                trailing = {
                    Switch(
                        checked = panicWipeEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                showPanicWipeConfirmDialog = true
                            } else {
                                viewModel.setPanicWipeEnabled(false)
                            }
                        }
                    )
                }
            )

            AnimatedVisibility(visible = panicWipeEnabled, enter = fadeIn(), exit = fadeOut()) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Wipe after failed attempts",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        PANIC_WIPE_THRESHOLD_OPTIONS.forEachIndexed { index, threshold ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = PANIC_WIPE_THRESHOLD_OPTIONS.size),
                                onClick = { viewModel.setPanicWipeThreshold(threshold) },
                                selected = panicWipeThreshold == threshold
                            ) {
                                Text("$threshold")
                            }
                        }
                    }
                    Text(
                        text = "All entries will be permanently deleted after $panicWipeThreshold consecutive failed attempts across all entries. Successful verification resets the counter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    if (!viewModel.hasBackup()) {
                        Text(
                            text = "Consider creating a backup first. Panic wipe will permanently delete all data.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            SettingsDivider()

            // --- Default Reminder Section ---
            SectionHeader(icon = { Icon(Icons.Filled.Timer, contentDescription = null) }, title = "Default Reminder")
            Text(
                text = "Default interval for new entries (days)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            val reminderOptions = listOf(1, 3, 5, 7, 14, 30)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                reminderOptions.forEachIndexed { index, days ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = reminderOptions.size),
                        onClick = { viewModel.setDefaultReminderDays(days) },
                        selected = defaultReminderDays == days
                    ) {
                        Text("$days")
                    }
                }
            }
            Text(
                text = "You'll be reminded to verify every $defaultReminderDays days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )

            SettingsDivider()

            // --- Notifications Section ---
            SectionHeader(icon = { Icon(Icons.Filled.Notifications, contentDescription = null) }, title = "Notifications")
            SettingsRow(
                title = "Enable Notifications",
                subtitle = "Receive reminders when passwords are due",
                trailing = {
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                    )
                }
            )

            AnimatedVisibility(visible = notificationsEnabled, enter = fadeIn(), exit = fadeOut()) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Quiet Hours",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { showStartTimePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Schedule, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("From: ${formatHour(quietHoursStart)}")
                        }
                        FilledTonalButton(
                            onClick = { showEndTimePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Schedule, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Until: ${formatHour(quietHoursEnd)}")
                        }
                    }
                    Text(
                        text = "No notifications between ${formatHour(quietHoursStart)} and ${formatHour(quietHoursEnd)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            SettingsDivider()

            // --- Appearance Section ---
            SectionHeader(icon = { Icon(Icons.Filled.Palette, contentDescription = null) }, title = "Appearance")
            Text(
                text = "Theme",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            val themeOptions = listOf("System" to SettingsViewModel.THEME_SYSTEM, "Light" to SettingsViewModel.THEME_LIGHT, "Dark" to SettingsViewModel.THEME_DARK)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                themeOptions.forEachIndexed { index, (label, mode) ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = themeOptions.size),
                        onClick = { viewModel.setThemeMode(mode) },
                        selected = themeMode == mode,
                        icon = {
                            when (mode) {
                                SettingsViewModel.THEME_LIGHT -> Icon(Icons.Filled.LightMode, contentDescription = null, modifier = Modifier.height(18.dp))
                                SettingsViewModel.THEME_DARK -> Icon(Icons.Filled.DarkMode, contentDescription = null, modifier = Modifier.height(18.dp))
                                else -> {}
                            }
                        }
                    ) {
                        Text(label)
                    }
                }
            }

            SettingsDivider()

            // --- Challenge Section ---
            SectionHeader(icon = { Icon(Icons.Filled.FitnessCenter, contentDescription = null) }, title = "Challenge")
            SettingsRow(
                title = "Adaptive difficulty",
                subtitle = "Increases challenge difficulty as your streak grows",
                trailing = {
                    Switch(
                        checked = adaptiveDifficulty,
                        onCheckedChange = { viewModel.setAdaptiveDifficulty(it) }
                    )
                }
            )

            SettingsDivider()

            // --- Help Section ---
            SectionHeader(icon = { Icon(Icons.Filled.AutoStories, contentDescription = null) }, title = "Help")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        navController.navigate(Routes.onboarding(isReplay = true))
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "View onboarding tutorial",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsDivider()

            // --- Backup & Restore Section ---
            SectionHeader(icon = { Icon(Icons.Filled.SwapVert, contentDescription = null) }, title = "Backup & Restore")
            FilledTonalButton(
                onClick = { showExportDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isExporting
            ) {
                if (isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exporting...")
                } else {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export encrypted backup")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            FilledTonalButton(
                onClick = { showImportWarningDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isImporting
            ) {
                if (isImporting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Importing...")
                } else {
                    Icon(Icons.Filled.CloudDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import backup")
                }
            }

            SettingsDivider()

            // --- Data Section ---
            SectionHeader(icon = { Icon(Icons.Filled.DeleteForever, contentDescription = null) }, title = "Data")
            FilledTonalButton(
                onClick = { showDeleteDialog = true },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete All Data")
            }

            SettingsDivider()

            // --- About Section ---
            SectionHeader(icon = { Icon(Icons.Filled.Info, contentDescription = null) }, title = "About")
            val versionName = remember {
                try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
                } catch (_: PackageManager.NameNotFoundException) {
                    "1.0"
                }
            }
            Text(
                text = "MPT v$versionName",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Filled.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Your passwords never leave this device. No internet access, no cloud sync, no analytics. Passwords are hashed with Argon2id and can never be recovered \u2014 not even by this app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // --- Delete All Data Dialog ---
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                deleteConfirmText = ""
            },
            icon = { Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete All Data?") },
            text = {
                Column {
                    Text(
                        text = "This will permanently delete all your stored passwords, settings, and reset the app. This cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Type DELETE to confirm:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = deleteConfirmText,
                        onValueChange = { deleteConfirmText = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = deleteConfirmText.isNotEmpty() && deleteConfirmText != "DELETE"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllData()
                        showDeleteDialog = false
                        deleteConfirmText = ""
                        navController.navigate(Routes.onboarding()) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    enabled = deleteConfirmText == "DELETE",
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    deleteConfirmText = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Time Picker Dialogs ---
    if (showStartTimePicker) {
        TimePickerDialog(
            title = "Quiet Hours Start",
            initialHour = quietHoursStart,
            onConfirm = { hour ->
                viewModel.setQuietHoursStart(hour)
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            title = "Quiet Hours End",
            initialHour = quietHoursEnd,
            onConfirm = { hour ->
                viewModel.setQuietHoursEnd(hour)
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false }
        )
    }

    // --- Export Backup Dialog ---
    if (showExportDialog) {
        BackupPasswordDialog(
            title = "Create backup password",
            confirmLabel = "Create Backup",
            isConfirm = true,
            onConfirm = { password ->
                showExportDialog = false
                viewModel.createBackup(password)
            },
            onDismiss = { showExportDialog = false }
        )
    }

    // --- Import Warning Dialog ---
    if (showImportWarningDialog) {
        AlertDialog(
            onDismissRequest = { showImportWarningDialog = false },
            icon = { Icon(Icons.Filled.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Replace all data?") },
            text = {
                Text("Importing a backup will REPLACE all current data. This cannot be undone. Continue?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportWarningDialog = false
                        importLauncher.launch(arrayOf("*/*"))
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportWarningDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Panic Wipe Confirmation Dialog ---
    if (showPanicWipeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPanicWipeConfirmDialog = false },
            icon = { Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Enable panic wipe?") },
            text = {
                Text("Are you sure? If you fail $panicWipeThreshold consecutive times, all your data will be permanently deleted with no way to recover it. Make sure you have a backup.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPanicWipeConfirmDialog = false
                        viewModel.setPanicWipeEnabled(true)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPanicWipeConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Import Password Dialog ---
    if (showImportPasswordDialog) {
        BackupPasswordDialog(
            title = "Enter backup password",
            confirmLabel = "Restore",
            isConfirm = false,
            onConfirm = { password ->
                showImportPasswordDialog = false
                val bytes = importFileBytes
                if (bytes != null) {
                    viewModel.importBackup(bytes, password)
                    importFileBytes = null
                }
            },
            onDismiss = {
                showImportPasswordDialog = false
                importFileBytes = null
            }
        )
    }
}

@Composable
private fun SectionHeader(icon: @Composable () -> Unit, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    ) {
        icon()
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailing()
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    title: String,
    initialHour: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = 0, is24Hour = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun BackupPasswordDialog(
    title: String,
    confirmLabel: String,
    isConfirm: Boolean,
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val passwordsMatch = !isConfirm || password == confirmPassword
    val canConfirm = password.isNotEmpty() && (!isConfirm || (confirmPassword.isNotEmpty() && passwordsMatch))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (isConfirm) {
                    Text(
                        text = "This password encrypts your backup file. You'll need it to restore. It can be different from your master passwords.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (isConfirm) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = confirmPassword.isNotEmpty() && !passwordsMatch,
                        supportingText = if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                            { Text("Passwords don't match") }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password.toCharArray()) },
                enabled = canConfirm
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatHour(hour: Int): String {
    return String.format("%02d:00", hour)
}
