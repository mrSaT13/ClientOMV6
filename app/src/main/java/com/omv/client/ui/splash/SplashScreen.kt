package com.omv.client.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omv.client.data.security.SecurePrefs
import com.omv.client.data.repository.OmvRepository
import com.omv.client.ui.theme.*
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val securePrefs: SecurePrefs,
    private val repository: OmvRepository
) : androidx.lifecycle.ViewModel() {
    val isFirstLaunch: Boolean get() = securePrefs.isFirstLaunch
    val hasCredentials: Boolean get() = securePrefs.rememberMe && securePrefs.hostPort.isNotEmpty() && securePrefs.username.isNotEmpty()
    val hasValidSession: Boolean get() = securePrefs.hasValidSession()

    suspend fun autoLogin(): Boolean {
        if (!hasCredentials) return false
        return try {
            repository.reconnect()
            val result = repository.login(securePrefs.username, securePrefs.password)
            result.isSuccess
        } catch (_: Exception) {
            false
        }
    }
}

@Composable
fun SplashScreen(
    onSplashComplete: (Boolean, Boolean) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    var autoLoginAttempted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        coroutineScope.launch {
            alpha.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))
        }
        coroutineScope.launch {
            delay(400)
            textAlpha.animateTo(1f, animationSpec = tween(500))
        }

        // Auto-login if credentials saved but session expired
        if (viewModel.hasCredentials && !viewModel.hasValidSession && !autoLoginAttempted) {
            autoLoginAttempted = true
            viewModel.autoLogin()
        }

        delay(2000)
        onSplashComplete(viewModel.isFirstLaunch, viewModel.hasCredentials)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Blue900, Blue700, Blue500)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .scale(scale.value)
                    .alpha(alpha.value)
                    .size(120.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(64.dp)
                        .scale(scale.value)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "OMV Client",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Управление NAS с телефона",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                modifier = Modifier
                    .size(32.dp)
                    .alpha(alpha.value),
                color = Color.White,
                strokeWidth = 3.dp
            )
        }
    }
}
