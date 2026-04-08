package com.mpt.masterpasswordtrainer.ui.screens.challenge

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Lock
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
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeScreen(
    navController: NavHostController,
    entryId: String,
    viewModel: ChallengeViewModel = viewModel()
) {
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
        if (viewModel.verificationResult == VerificationResult.EMAIL_WRONG) {
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
                        text = "Perfect! ✓",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF43A047)
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
                        text = "Type your credentials from memory",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Email field with shake
                    val emailBorderColor by animateColorAsState(
                        targetValue = when (viewModel.verificationResult) {
                            VerificationResult.EMAIL_WRONG -> Color(0xFFFFA000)
                            VerificationResult.BOTH_WRONG -> Color(0xFFE53935)
                            else -> MaterialTheme.colorScheme.outline
                        },
                        label = "emailBorder"
                    )

                    OutlinedTextField(
                        value = viewModel.emailInput,
                        onValueChange = { viewModel.updateEmailInput(it) },
                        label = { Text("Email address") },
                        singleLine = true,
                        enabled = !viewModel.isLocked && !viewModel.isVerifying,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { passwordFocusRequester.requestFocus() }
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

                    // Password field with shake
                    val passwordBorderColor by animateColorAsState(
                        targetValue = when (viewModel.verificationResult) {
                            VerificationResult.PASSWORD_WRONG,
                            VerificationResult.BOTH_WRONG -> Color(0xFFE53935)
                            else -> MaterialTheme.colorScheme.outline
                        },
                        label = "passwordBorder"
                    )

                    // Password as string for display, backed by CharArray
                    var passwordText by remember { mutableStateOf("") }

                    // Sync passwordText when viewModel clears it
                    LaunchedEffect(viewModel.passwordInput.size) {
                        if (viewModel.passwordInput.isEmpty()) {
                            passwordText = ""
                        }
                    }

                    OutlinedTextField(
                        value = passwordText,
                        onValueChange = { newValue ->
                            passwordText = newValue
                            viewModel.updatePasswordInput(newValue.toCharArray())
                        },
                        label = { Text("Master password") },
                        singleLine = true,
                        enabled = !viewModel.isLocked && !viewModel.isVerifying,
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { viewModel.verify() }
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

                    // Email hint option
                    AnimatedVisibility(
                        visible = viewModel.showHintOption && !viewModel.showSuccess,
                        enter = fadeIn() + slideInVertically { it / 2 }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            if (viewModel.maskedEmailHint == null) {
                                TextButton(onClick = { viewModel.showEmailHint() }) {
                                    Text(
                                        "Tap for email hint",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                Text(
                                    text = "Hint: ${viewModel.maskedEmailHint}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            enabled = !viewModel.isVerifying &&
                                    viewModel.emailInput.isNotBlank() &&
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
