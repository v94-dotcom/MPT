package com.mpt.masterpasswordtrainer.ui.screens.challenge

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.mpt.masterpasswordtrainer.ui.components.SuccessAnimation
import com.mpt.masterpasswordtrainer.ui.components.getIconForName
import com.mpt.masterpasswordtrainer.ui.components.shakeAnimation
import com.mpt.masterpasswordtrainer.ui.navigation.Routes
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeScreen(
    navController: NavHostController,
    entryId: String,
    viewModel: ChallengeViewModel = viewModel()
) {
    // Silent navigation on panic wipe — app appears as fresh install
    LaunchedEffect(viewModel.panicWipeTriggered) {
        if (viewModel.panicWipeTriggered) {
            navController.navigate(Routes.onboarding()) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    LaunchedEffect(entryId) {
        viewModel.loadEntry(entryId)
    }

    val entry = viewModel.entry
    if (entry == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val accentColor = Color(entry.serviceColor)

    // Auto-return to dashboard on success
    LaunchedEffect(viewModel.showSuccess) {
        if (viewModel.showSuccess) {
            delay(2500)
            navController.popBackStack()
        }
    }

    // Background flash color
    val bgColor by animateColorAsState(
        targetValue = when (viewModel.verificationResult) {
            VerificationResult.SUCCESS -> Color(0xFF43A047).copy(alpha = 0.08f)
            VerificationResult.EMAIL_WRONG -> Color(0xFFFFA000).copy(alpha = 0.08f)
            VerificationResult.PASSWORD_WRONG,
            VerificationResult.BOTH_WRONG -> Color(0xFFE53935).copy(alpha = 0.08f)
            null -> MaterialTheme.colorScheme.background
        },
        animationSpec = tween(300),
        label = "bgColor"
    )

    // Streak scale animation
    val streakScale by animateFloatAsState(
        targetValue = if (viewModel.showSuccess) 1.2f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "streakScale"
    )

    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    var passwordVisible by remember { mutableStateOf(false) }

    // Focus email field after email-wrong result
    LaunchedEffect(viewModel.verificationResult) {
        if (viewModel.hasEmail && viewModel.verificationResult == VerificationResult.EMAIL_WRONG) {
            delay(450) // After shake animation
            emailFocusRequester.requestFocus()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(paddingValues)
        ) {
            // Success overlay
            AnimatedVisibility(
                visible = viewModel.showSuccess,
                enter = fadeIn(tween(300)) + scaleIn(
                    spring(Spring.DampingRatioMediumBouncy)
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    SuccessAnimation(accentColor = accentColor)

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = viewModel.resultMessage ?: "Perfect! ✓",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF43A047),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.graphicsLayer {
                            scaleX = streakScale
                            scaleY = streakScale
                        }
                    ) {
                        Icon(
                            Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${entry.streak} streak",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Main challenge UI (hidden during success)
            AnimatedVisibility(
                visible = !viewModel.showSuccess,
                exit = fadeOut(tween(200)),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Service icon + name header
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getIconForName(entry.serviceIcon),
                            contentDescription = entry.serviceName,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = entry.serviceName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (viewModel.hasEmail) "Type your credentials from memory"
                               else "Type your password from memory",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    // Difficulty banner
                    val difficulty = viewModel.difficultyTier
                    AnimatedVisibility(
                        visible = difficulty != DifficultyTier.NORMAL,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(accentColor.copy(alpha = 0.12f))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (difficulty == DifficultyTier.ADVANCED) Icons.Filled.Psychology else Icons.Filled.SwapVert,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (difficulty == DifficultyTier.ADVANCED) "Delayed recall — advanced mode" else "Reversed order — intermediate mode",
                                style = MaterialTheme.typography.labelMedium,
                                color = accentColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Delay countdown overlay for advanced mode
                    AnimatedVisibility(
                        visible = viewModel.isDelayActive,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { viewModel.delayCountdown / DifficultyTier.ADVANCED_DELAY_SECONDS.toFloat() },
                                    modifier = Modifier.size(80.dp),
                                    color = accentColor,
                                    strokeWidth = 4.dp
                                )
                                Text(
                                    text = "${viewModel.delayCountdown}",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Recall your credentials...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Reversed field order: intermediate/advanced show password first
                    val reversed = difficulty != DifficultyTier.NORMAL
                    val fieldsEnabled = !viewModel.isLocked && !viewModel.isVerifying && !viewModel.isDelayActive

                    // Email field
                    val emailBorderColor by animateColorAsState(
                        targetValue = when (viewModel.verificationResult) {
                            VerificationResult.EMAIL_WRONG -> Color(0xFFFFA000)
                            VerificationResult.BOTH_WRONG -> Color(0xFFE53935)
                            else -> MaterialTheme.colorScheme.outline
                        },
                        label = "emailBorder"
                    )

                    // Password field state
                    val passwordBorderColor by animateColorAsState(
                        targetValue = when (viewModel.verificationResult) {
                            VerificationResult.PASSWORD_WRONG,
                            VerificationResult.BOTH_WRONG -> Color(0xFFE53935)
                            else -> MaterialTheme.colorScheme.outline
                        },
                        label = "passwordBorder"
                    )

                    var passwordText by remember { mutableStateOf("") }

                    LaunchedEffect(viewModel.passwordInput.size) {
                        if (viewModel.passwordInput.isEmpty()) {
                            passwordText = ""
                        }
                    }

                    // Composable lambdas for reordering
                    val emailField: @Composable () -> Unit = {
                        if (viewModel.hasEmail) {
                            OutlinedTextField(
                                value = viewModel.emailInput,
                                onValueChange = { viewModel.updateEmailInput(it) },
                                label = { Text("Email / Username") },
                                singleLine = true,
                                enabled = fieldsEnabled,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = if (reversed) ImeAction.Done else ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { passwordFocusRequester.requestFocus() },
                                    onDone = { if (reversed) viewModel.verify() }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = emailBorderColor,
                                    unfocusedBorderColor = emailBorderColor
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(emailFocusRequester)
                                    .shakeAnimation(
                                        trigger = viewModel.shakeEmail,
                                        onComplete = { viewModel.onShakeEmailComplete() }
                                    )
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    val passwordField: @Composable () -> Unit = {
                        OutlinedTextField(
                            value = passwordText,
                            onValueChange = { newValue ->
                                passwordText = newValue
                                viewModel.updatePasswordInput(newValue.toCharArray())
                            },
                            label = { Text("Master password") },
                            singleLine = true,
                            enabled = fieldsEnabled,
                            visualTransformation = if (passwordVisible)
                                VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = if (reversed) ImeAction.Next else ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { if (reversed) emailFocusRequester.requestFocus() },
                                onDone = { if (!reversed) viewModel.verify() }
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
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = passwordBorderColor,
                                unfocusedBorderColor = passwordBorderColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(passwordFocusRequester)
                                .shakeAnimation(
                                    trigger = viewModel.shakePassword,
                                    onComplete = { viewModel.onShakePasswordComplete() }
                                )
                        )
                    }

                    if (reversed) {
                        passwordField()
                        Spacer(modifier = Modifier.height(16.dp))
                        emailField()
                    } else {
                        emailField()
                        passwordField()
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Result message
                    AnimatedVisibility(
                        visible = viewModel.resultMessage != null && !viewModel.showSuccess,
                        enter = fadeIn() + slideInVertically { it / 2 }
                    ) {
                        val messageColor = when (viewModel.verificationResult) {
                            VerificationResult.SUCCESS -> Color(0xFF43A047)
                            VerificationResult.EMAIL_WRONG -> Color(0xFFFFA000)
                            VerificationResult.PASSWORD_WRONG,
                            VerificationResult.BOTH_WRONG -> Color(0xFFE53935)
                            null -> MaterialTheme.colorScheme.onSurface
                        }

                        Text(
                            text = viewModel.resultMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = messageColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }

                    // Hint options (after 3 failures)
                    AnimatedVisibility(
                        visible = viewModel.showHintOption && !viewModel.showSuccess,
                        enter = fadeIn() + expandVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Login hint row
                            if (viewModel.hasEmail) {
                                HintRow(
                                    icon = Icons.Filled.Mail,
                                    label = "Show login hint",
                                    revealedText = viewModel.maskedEmailHint,
                                    accentColor = accentColor,
                                    onClick = { viewModel.showEmailHint() }
                                )
                            }

                            // Password hint row
                            if (viewModel.hasPasswordHint) {
                                HintRow(
                                    icon = Icons.Filled.Lightbulb,
                                    label = "Show password hint",
                                    revealedText = viewModel.passwordHintRevealed,
                                    accentColor = accentColor,
                                    onClick = { viewModel.showPasswordHint() }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Verify / Lock button
                    if (viewModel.isLocked) {
                        // Locked state
                        Button(
                            onClick = { },
                            enabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Locked for ${viewModel.lockTimeRemaining}s",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    } else {
                        Button(
                            onClick = { viewModel.verify() },
                            enabled = !viewModel.isVerifying && !viewModel.isDelayActive &&
                                    (!viewModel.hasEmail || viewModel.emailInput.isNotBlank()) &&
                                    passwordText.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor
                            )
                        ) {
                            if (viewModel.isVerifying) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    "Verify",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Bottom info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val daysText = when (viewModel.daysElapsed) {
                            0 -> "Verified today"
                            1 -> "Last verified: 1 day ago"
                            else -> "Last verified: ${viewModel.daysElapsed} days ago"
                        }
                        Text(
                            text = daysText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Attempts: ${viewModel.sessionAttempts}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun HintRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    revealedText: String?,
    accentColor: Color,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + expandVertically()
    ) {
        if (revealedText == null) {
            // Unrevealed: tappable text button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            // Revealed: hint text with background
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = revealedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
