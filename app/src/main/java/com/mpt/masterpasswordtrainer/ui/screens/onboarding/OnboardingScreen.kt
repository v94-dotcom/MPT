package com.mpt.masterpasswordtrainer.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.mpt.masterpasswordtrainer.ui.components.*
import com.mpt.masterpasswordtrainer.ui.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    navController: NavHostController,
    viewModel: OnboardingViewModel = viewModel()
) {
    val currentPage by viewModel.currentPage.collectAsState()
    val pagerState = rememberPagerState(pageCount = { viewModel.totalPages })
    val coroutineScope = rememberCoroutineScope()

    // Sync pager with viewmodel
    LaunchedEffect(currentPage) {
        if (pagerState.currentPage != currentPage) {
            pagerState.animateScrollToPage(currentPage)
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        viewModel.setPage(pagerState.currentPage)
    }

    val isLastPage = currentPage == viewModel.totalPages - 1

    Box(modifier = Modifier.fillMaxSize()) {
        // Skip button — top right (hidden on last page)
        if (!isLastPage) {
            TextButton(
                onClick = {
                    viewModel.skipToEnd()
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(viewModel.totalPages - 1)
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 16.dp)
            ) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val isVisible = pagerState.currentPage == page
                when (page) {
                    0 -> OnboardingPage(
                        visual = { PasswordFadeAnimation() },
                        headline = "When did you last type your master password?",
                        body = "You set it once. Now you use fingerprint or a PIN. But what happens when you get a new phone, reinstall your vault, or biometrics fail?",
                        isVisible = isVisible
                    )
                    1 -> OnboardingPage(
                        visual = { CycleAnimation() },
                        headline = "Train your memory like a muscle",
                        body = "MPT reminds you every few days to type your master password and email from memory. Get it right? Your streak grows. Forget? You'll know before it matters.",
                        isVisible = isVisible
                    )
                    2 -> OnboardingPage(
                        visual = { ShieldBounceAnimation() },
                        headline = "Your password never leaves this device",
                        extraContent = {
                            TrustPoints()
                        },
                        isVisible = isVisible
                    )
                    3 -> OnboardingPage(
                        visual = { LogoPulseAnimation() },
                        headline = "Ready to train?",
                        body = "Add your first master password and set up your practice schedule. It takes 30 seconds.",
                        extraContent = {
                            Button(
                                onClick = {
                                    navController.navigate(Routes.addEntry(isFromOnboarding = true))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = "Add My First Password",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null
                                )
                            }
                        },
                        isVisible = isVisible
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Progress dots
            ProgressDots(
                totalPages = viewModel.totalPages,
                currentPage = currentPage
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Next button (hidden on last page — replaced by CTA in page content)
            if (!isLastPage) {
                Button(
                    onClick = {
                        viewModel.nextPage()
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(currentPage + 1)
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Next",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                // Keep space so dots don't jump
                Spacer(modifier = Modifier.height(52.dp))
            }
        }
    }
}

@Composable
private fun TrustPoints() {
    val points = listOf(
        Triple(Icons.Filled.Lock, "Your password is hashed — even this app can't see it", 0),
        Triple(Icons.Filled.WifiOff, "Zero internet access — no network permissions at all", 100),
        Triple(Icons.Filled.Shield, "Encrypted storage backed by your phone's security chip", 200),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        points.forEach { (icon, text, _) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ProgressDots(
    totalPages: Int,
    currentPage: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalPages) { index ->
            val isActive = index == currentPage

            val size by animateDpAsState(
                targetValue = if (isActive) 8.dp else 6.dp,
                animationSpec = spring(dampingRatio = 0.7f),
                label = "dotSize$index"
            )

            val color by animateColorAsState(
                targetValue = if (isActive)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                animationSpec = tween(300),
                label = "dotColor$index"
            )

            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .then(
                        if (isActive) {
                            Modifier.background(color)
                        } else {
                            Modifier.border(1.dp, color, CircleShape)
                        }
                    )
            )
        }
    }
}
