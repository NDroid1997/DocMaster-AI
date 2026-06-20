package com.ndroid.docmasterai.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ndroid.docmasterai.ui.theme.GradientEnd
import com.ndroid.docmasterai.ui.theme.GradientMid
import com.ndroid.docmasterai.ui.theme.GradientStart
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsState(initial = true)

    // Animations
    val scale = remember { Animatable(0.3f) }
    val alpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(800))
    }
    LaunchedEffect(Unit) {
        delay(400)
        textAlpha.animateTo(1f, animationSpec = tween(600))
    }

    LaunchedEffect(isFirstLaunch) {
        delay(2200)
        if (isFirstLaunch) onNavigateToOnboarding() else onNavigateToHome()
    }

    val gradient = Brush.linearGradient(
        colors = listOf(GradientStart, GradientMid, GradientEnd),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .scale(scale.value)
                    .alpha(alpha.value),
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            brush = gradient,
                            shape = MaterialTheme.shapes.large,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.DocumentScanner,
                        contentDescription = "DocMaster AI",
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "DocMaster AI",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alpha(textAlpha.value),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Scan • Convert • Organize",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(textAlpha.value),
                letterSpacing = 2.sp,
            )
        }
    }
}
