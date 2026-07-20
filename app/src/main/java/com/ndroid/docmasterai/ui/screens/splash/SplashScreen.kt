package com.ndroid.docmasterai.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImagePainter
import com.ndroid.docmasterai.ui.theme.GradientEnd
import com.ndroid.docmasterai.ui.theme.GradientMid
import com.ndroid.docmasterai.ui.theme.GradientStart
import com.ndroid.docmasterai.ui.theme.LogoBackground
import kotlinx.coroutines.delay
import com.ndroid.docmasterai.R

@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsState(initial = true)
    val isLoggedIn by viewModel.isLoggedIn.collectAsState(initial = false)

    // Animations
    val scale = remember { Animatable(1f) }
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

    LaunchedEffect(isFirstLaunch, isLoggedIn) {
        delay(2200)
        if (isFirstLaunch) {
            onNavigateToOnboarding()
        } else if (!isLoggedIn) {
            onNavigateToLogin()
        } else {
            onNavigateToHome()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LogoBackground),
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
                AppLogo()
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

//@Composable
//@Preview(heightDp = 640, widthDp = 360)
//fun splashScreen(){
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color(0xFF060030)),
//        contentAlignment = Alignment.Center,
//    ) {
//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Center,
//        ) {
//            Box(
//                modifier = Modifier
//                    .scale(1f)
//                    .alpha(1f),
//            ) {
//                AppLogo()
//            }
//
//            Spacer(modifier = Modifier.height(24.dp))
//
//            Text(
//                text = "DocMaster AI",
//                style = MaterialTheme.typography.headlineLarge,
//                fontWeight = FontWeight.Bold,
//                color = MaterialTheme.colorScheme.onBackground,
//                modifier = Modifier.alpha(1f),
//            )
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            Text(
//                text = "Scan • Convert • Organize",
//                style = MaterialTheme.typography.bodyLarge,
//                color = MaterialTheme.colorScheme.onSurfaceVariant,
//                modifier = Modifier.alpha(1f),
//                letterSpacing = 2.sp,
//            )
//        }
//    }
//}
@Composable
fun AppLogo() {
    Box(
        modifier = Modifier
            .size(120.dp)
            .background(
                color = LogoBackground,
                shape = MaterialTheme.shapes.large,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = "DocMaster AI",
            contentScale = ContentScale.Fit
        )
    }
}
