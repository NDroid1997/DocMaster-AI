package com.ndroid.docmasterai.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ndroid.docmasterai.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val gradientColors: List<androidx.compose.ui.graphics.Color>,
)

val onboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Filled.DocumentScanner,
        title = "Scan Documents",
        description = "Quickly scan documents with automatic edge detection, perspective correction, and professional image filters.",
        gradientColors = listOf(CardGradient1Start, CardGradient1End),
    ),
    OnboardingPage(
        icon = Icons.Filled.Transform,
        title = "Convert & Extract",
        description = "Convert between formats, extract text with OCR, merge PDFs, and resize images — all in one app.",
        gradientColors = listOf(CardGradient2Start, CardGradient2End),
    ),
    OnboardingPage(
        icon = Icons.Filled.FolderOpen,
        title = "Organize & Share",
        description = "Keep your documents organized with folders, favorites, and smart search. Share anywhere instantly.",
        gradientColors = listOf(CardGradient4Start, CardGradient4End),
    ),
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Skip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = {
                    viewModel.completeOnboarding()
                    onFinish()
                }) {
                    Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                OnboardingPageContent(onboardingPages[page])
            }

            // Indicators + Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Page indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(onboardingPages.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (pagerState.currentPage == index) 24.dp else 8.dp, 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outlineVariant
                                ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Action button
                Button(
                    onClick = {
                        if (pagerState.currentPage < onboardingPages.size - 1) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            viewModel.completeOnboarding()
                            onFinish()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text(
                        text = if (pagerState.currentPage == onboardingPages.size - 1) "Get Started" else "Next",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = page.gradientColors,
                        start = Offset(0f, 0f),
                        end = Offset(300f, 300f),
                    ),
                    shape = MaterialTheme.shapes.extraLarge,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = page.title,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
        )
    }
}
