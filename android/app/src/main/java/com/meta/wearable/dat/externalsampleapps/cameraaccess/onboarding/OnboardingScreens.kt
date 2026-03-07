package com.meta.wearable.dat.externalsampleapps.cameraaccess.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingFlow(onFinished: () -> Unit) {
    var currentStep by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentStep) {
        if (currentStep == 0) {
            delay(2000) // Splash delay
            currentStep = 1
        }
    }

    Scaffold(
        containerColor = Color.White
    ) { padding ->
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) + fadeIn() togetherWith
                        slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400)) + fadeOut()
                } else {
                    slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) + fadeIn() togetherWith
                        slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)) + fadeOut()
                }
            },
            modifier = Modifier.padding(padding)
        ) { step ->
            when (step) {
                0 -> SplashScreen()
                1 -> WelcomeScreen(onGetStarted = { currentStep = 2 })
                2 -> GenderScreen(
                    onBack = { currentStep = 1 },
                    onContinue = { currentStep = 3 }
                )
                3 -> WorkoutsScreen(
                    onBack = { currentStep = 2 },
                    onContinue = { currentStep = 4 }
                )
                4 -> ChartScreen(
                    onBack = { currentStep = 3 },
                    onContinue = onFinished
                )
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "?? Gymbro",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        // Placeholder for camera viewfinder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.6f)
                .clip(RoundedCornerShape(32.dp))
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Camera Preview", color = Color.DarkGray)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "AI Gym Assistant\nmade easy",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color.Black,
            lineHeight = 36.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            shape = CircleShape
        ) {
            Text("Get Started", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Already have an account? Sign In",
            fontSize = 16.sp,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun OnboardingHeader(
    title: String,
    subtitle: String,
    progress: Float,
    onBack: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape),
                color = Color.Black,
                trackColor = Color(0xFFE0E0E0),
            )
            Spacer(modifier = Modifier.width(48.dp))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 24.dp),
            lineHeight = 40.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = subtitle,
            fontSize = 18.sp,
            color = Color.DarkGray,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
fun GenderScreen(onBack: () -> Unit, onContinue: () -> Unit) {
    var selected by remember { mutableStateOf<String?>(null) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        OnboardingHeader(
            title = "Choose your Gender",
            subtitle = "This will be used to calibrate your custom plan.",
            progress = 0.25f,
            onBack = onBack
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            SelectionCard("Male", isSelected = selected == "Male") { selected = "Male" }
            SelectionCard("Female", isSelected = selected == "Female") { selected = "Female" }
            SelectionCard("Other", isSelected = selected == "Other") { selected = "Other" }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        ContinueButton(enabled = selected != null, onClick = onContinue)
    }
}

@Composable
fun WorkoutsScreen(onBack: () -> Unit, onContinue: () -> Unit) {
    var selected by remember { mutableStateOf<String?>(null) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        OnboardingHeader(
            title = "How many workouts do you do per week?",
            subtitle = "This will be used to calibrate your custom plan.",
            progress = 0.5f,
            onBack = onBack
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            SelectionCardWithSubtext("0-2", "Workouts now and then", isSelected = selected == "0-2") { selected = "0-2" }
            SelectionCardWithSubtext("3-5", "A few workouts per week", isSelected = selected == "3-5") { selected = "3-5" }
            SelectionCardWithSubtext("6+", "Dedicated athlete", isSelected = selected == "6+") { selected = "6+" }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        ContinueButton(enabled = selected != null, onClick = onContinue)
    }
}

@Composable
fun ChartScreen(onBack: () -> Unit, onContinue: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        OnboardingHeader(
            title = "Gymbro creates\nlong-term results",
            subtitle = "",
            progress = 0.75f,
            onBack = onBack
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFF7F7F7))
                .padding(24.dp)
        ) {
            Column {
                Text("Your progress", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                Spacer(modifier = Modifier.height(24.dp))
                MockChartCanvas()
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Month 1", fontSize = 14.sp, color = Color.DarkGray)
                    Text("Month 6", fontSize = 14.sp, color = Color.DarkGray)
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    "80% of Gymbro users maintain their fitness goals even 6 months later",
                    fontSize = 16.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        ContinueButton(enabled = true, onClick = onContinue)
    }
}

@Composable
fun MockChartCanvas() {
    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        val width = size.width
        val height = size.height
        
        drawLine(
            color = Color.LightGray,
            start = Offset(0f, height * 0.25f),
            end = Offset(width, height * 0.25f),
            strokeWidth = 2f
        )
        drawLine(
            color = Color.LightGray,
            start = Offset(0f, height * 0.75f),
            end = Offset(width, height * 0.75f),
            strokeWidth = 2f
        )
        
        val tradPath = Path().apply {
            moveTo(0f, height * 0.25f)
            cubicTo(
                width * 0.3f, height * 0.25f,
                width * 0.5f, height * 0.8f,
                width, height * 0.1f
            )
        }
        drawPath(tradPath, color = Color(0xFFFF8E8E), style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))

        val gymbroPath = Path().apply {
            moveTo(0f, height * 0.25f)
            cubicTo(
                width * 0.4f, height * 0.3f,
                width * 0.6f, height * 0.9f,
                width, height * 0.95f
            )
        }
        drawPath(gymbroPath, color = Color.Black, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        
        drawCircle(color = Color.White, radius = 10f, center = Offset(0f, height * 0.25f))
        drawCircle(color = Color.Black, radius = 10f, center = Offset(0f, height * 0.25f), style = Stroke(width = 4f))
        
        drawCircle(color = Color.White, radius = 10f, center = Offset(width, height * 0.95f))
        drawCircle(color = Color.Black, radius = 10f, center = Offset(width, height * 0.95f), style = Stroke(width = 4f))
    }
}

@Composable
fun SelectionCard(title: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) Color.Black else Color(0xFFF7F7F7)
    val textColor = if (isSelected) Color.White else Color.Black
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = textColor)
    }
}

@Composable
fun SelectionCardWithSubtext(title: String, subtitle: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) Color.Black else Color(0xFFF7F7F7)
    val textColor = if (isSelected) Color.White else Color.Black
    val subTextColor = if (isSelected) Color.LightGray else Color.DarkGray
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(vertical = 20.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isSelected) Color.White else Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text("•", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = textColor)
            Text(text = subtitle, fontSize = 14.sp, color = subTextColor)
        }
    }
}

@Composable
fun ContinueButton(enabled: Boolean, onClick: () -> Unit) {
    val alpha = if (enabled) 1f else 0.5f
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black.copy(alpha = alpha),
            disabledContainerColor = Color.Gray
        ),
        shape = CircleShape
    ) {
        Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

