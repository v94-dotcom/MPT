package com.mpt.masterpasswordtrainer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.mpt.masterpasswordtrainer.ui.navigation.MPTNavGraph
import com.mpt.masterpasswordtrainer.ui.navigation.getStartDestination
import com.mpt.masterpasswordtrainer.ui.screens.settings.SettingsViewModel
import com.mpt.masterpasswordtrainer.ui.theme.MPTTheme
import com.mpt.masterpasswordtrainer.ui.theme.ThemeState

class MainActivity : FragmentActivity() {

    private var navController: NavHostController? = null

    private var isAuthenticated by mutableStateOf(false)
    private var authFailed by mutableStateOf(false)
    private var lastBackgroundTime: Long = 0L

    private val appLockEnabled: Boolean
        get() {
            val prefs = getSharedPreferences("mpt_prefs", Context.MODE_PRIVATE)
            return prefs.getBoolean(SettingsViewModel.KEY_APP_LOCK_ENABLED, false)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        enableEdgeToEdge()

        ThemeState.initialize(this)

        val startDestination = getStartDestination(this)

        // If app lock is not enabled, mark as authenticated immediately
        if (!appLockEnabled) {
            isAuthenticated = true
        }

        setContent {
            MPTTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val controller = rememberNavController()
                    navController = controller

                    // Show the app content when authenticated
                    AnimatedVisibility(
                        visible = isAuthenticated,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        MPTNavGraph(
                            navController = controller,
                            startDestination = startDestination
                        )
                    }

                    // Show locked screen when not authenticated
                    AnimatedVisibility(
                        visible = !isAuthenticated,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Filled.Lock,
                                    contentDescription = "Locked",
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "MPT is Locked",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Authenticate to access your passwords",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (authFailed) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    FilledTonalButton(onClick = { showBiometricPrompt() }) {
                                        Icon(Icons.Filled.Fingerprint, contentDescription = null)
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Trigger biometric prompt on cold start if app lock is enabled
        if (appLockEnabled) {
            showBiometricPrompt()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navController?.handleDeepLink(intent)
    }

    override fun onStop() {
        super.onStop()
        lastBackgroundTime = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        if (!appLockEnabled) {
            isAuthenticated = true
            return
        }

        val elapsed = System.currentTimeMillis() - lastBackgroundTime
        if (lastBackgroundTime > 0 && elapsed > LOCK_TIMEOUT_MS) {
            isAuthenticated = false
            authFailed = false
            showBiometricPrompt()
        }
    }

    private fun showBiometricPrompt() {
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            // No biometric/device credential available — grant access
            isAuthenticated = true
            authFailed = false
            return
        }

        val executor = ContextCompat.getMainExecutor(this)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                isAuthenticated = true
                authFailed = false
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                authFailed = true
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Single attempt failed but prompt stays open — don't set authFailed yet
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock MPT")
            .setSubtitle("Authenticate to access your passwords")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val biometricPrompt = BiometricPrompt(this, executor, callback)
        biometricPrompt.authenticate(promptInfo)
    }

    companion object {
        private const val LOCK_TIMEOUT_MS = 30_000L // 30 seconds
    }
}
