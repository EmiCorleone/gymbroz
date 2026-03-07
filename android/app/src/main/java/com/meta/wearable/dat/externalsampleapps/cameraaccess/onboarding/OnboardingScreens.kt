package com.meta.wearable.dat.externalsampleapps.cameraaccess.onboarding

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.AppColor
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme.GradientButton
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme.GymBroTheme
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme.SelectionOption
import kotlinx.coroutines.delay
import java.io.File

// Total onboarding steps (not counting splash and welcome)
private const val TOTAL_STEPS = 8

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingFlow(
    onFinished: () -> Unit,
    onboardingViewModel: OnboardingViewModel = viewModel()
) {
    val state by onboardingViewModel.state.collectAsStateWithLifecycle()
    var currentStep by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentStep) {
        if (currentStep == 0) {
            delay(2500)
            currentStep = 1
        }
    }

    GymBroTheme {
        Scaffold(containerColor = AppColor.Background) { padding ->
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)) +
                            fadeIn(animationSpec = tween(300)) togetherWith
                            slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(400)) +
                            fadeOut(animationSpec = tween(200))
                    } else {
                        slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)) +
                            fadeIn(animationSpec = tween(300)) togetherWith
                            slideOutHorizontally(targetOffsetX = { it / 3 }, animationSpec = tween(400)) +
                            fadeOut(animationSpec = tween(200))
                    }
                },
                modifier = Modifier.padding(padding),
                label = "onboarding"
            ) { step ->
                when (step) {
                    0 -> SplashScreen()
                    1 -> WelcomeScreen(onGetStarted = { currentStep = 2 })
                    2 -> NameScreen(
                        name = state.name,
                        onNameChange = onboardingViewModel::updateName,
                        onBack = { currentStep = 1 },
                        onContinue = { currentStep = 3 }
                    )
                    3 -> GenderScreen(
                        selected = state.gender,
                        onSelect = onboardingViewModel::updateGender,
                        onBack = { currentStep = 2 },
                        onContinue = { currentStep = 4 }
                    )
                    4 -> BodyStatsScreen(
                        age = state.age,
                        height = state.heightCm,
                        weight = state.weightKg,
                        onAgeChange = onboardingViewModel::updateAge,
                        onHeightChange = onboardingViewModel::updateHeight,
                        onWeightChange = onboardingViewModel::updateWeight,
                        onBack = { currentStep = 3 },
                        onContinue = { currentStep = 5 }
                    )
                    5 -> FitnessGoalScreen(
                        selected = state.fitnessGoal,
                        onSelect = onboardingViewModel::updateFitnessGoal,
                        onBack = { currentStep = 4 },
                        onContinue = { currentStep = 6 }
                    )
                    6 -> ExperienceScreen(
                        selected = state.experienceLevel,
                        onSelect = onboardingViewModel::updateExperienceLevel,
                        onBack = { currentStep = 5 },
                        onContinue = { currentStep = 7 }
                    )
                    7 -> WorkoutsScreen(
                        selected = state.weeklyWorkouts,
                        onSelect = onboardingViewModel::updateWeeklyWorkouts,
                        onBack = { currentStep = 6 },
                        onContinue = { currentStep = 8 }
                    )
                    8 -> MirrorPhotoScreen(
                        photoPath = state.mirrorPhotoPath,
                        onPhotoTaken = onboardingViewModel::updateMirrorPhotoPath,
                        onBack = { currentStep = 7 },
                        onContinue = { currentStep = 9 }
                    )
                    9 -> ChartScreen(
                        onBack = { currentStep = 8 },
                        onContinue = { currentStep = 10 }
                    )
                    10 -> AllSetScreen(
                        name = state.name,
                        onFinish = { onboardingViewModel.saveProfile(onFinished) }
                    )
                }
            }
        }
    }
}

// ---- HEADER ----

@Composable
private fun OnboardingHeader(
    title: String,
    subtitle: String,
    progress: Float,
    stepNumber: Int,
    totalSteps: Int,
    onBack: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppColor.TextPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = AppColor.Accent,
                    trackColor = AppColor.CardBorder
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$stepNumber of $totalSteps",
                    fontSize = 12.sp,
                    color = AppColor.TextMuted,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
            Spacer(modifier = Modifier.width(48.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            color = AppColor.TextPrimary,
            modifier = Modifier.padding(horizontal = 24.dp),
            lineHeight = 36.sp
        )
        if (subtitle.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                fontSize = 16.sp,
                color = AppColor.TextSecondary,
                modifier = Modifier.padding(horizontal = 24.dp),
                lineHeight = 22.sp
            )
        }
    }
}

// ---- SPLASH ----

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(AppColor.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "\uD83C\uDFCB\uFE0F", fontSize = 72.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("GYMBRO", fontSize = 42.sp, fontWeight = FontWeight.Black, color = AppColor.TextPrimary, letterSpacing = 4.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("AI-Powered Fitness", fontSize = 16.sp, color = AppColor.Accent, fontWeight = FontWeight.Medium, letterSpacing = 2.sp)
        }
    }
}

// ---- WELCOME ----

@Composable
private fun WelcomeScreen(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(AppColor.Background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.8f))
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(0.75f)
                .clip(RoundedCornerShape(32.dp))
                .background(Brush.verticalGradient(listOf(AppColor.Surface, AppColor.CardBackground, AppColor.Background))),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("\uD83D\uDD76\uFE0F", fontSize = 80.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Your AI Coach\nLives Here", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColor.TextSecondary, textAlign = TextAlign.Center, lineHeight = 28.sp)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("AI Gym Assistant\nmade effortless", fontSize = 32.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, color = AppColor.TextPrimary, lineHeight = 38.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Real-time coaching through your Meta glasses.\nAutomatic rep counting. Zero friction.", fontSize = 15.sp, color = AppColor.TextSecondary, textAlign = TextAlign.Center, lineHeight = 22.sp)
        Spacer(modifier = Modifier.weight(1f))
        GradientButton(text = "Get Started", onClick = onGetStarted)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Already have an account? Sign In", fontSize = 14.sp, color = AppColor.TextMuted, modifier = Modifier.clickable { })
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ---- NAME ----

@Composable
private fun NameScreen(name: String, onNameChange: (String) -> Unit, onBack: () -> Unit, onContinue: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(AppColor.Background)) {
        OnboardingHeader("What's your name?", "We'll use this to personalize your experience.", 1f / TOTAL_STEPS, 1, TOTAL_STEPS, onBack)
        Spacer(modifier = Modifier.height(48.dp))
        OutlinedTextField(
            value = name, onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            placeholder = { Text("Enter your name", color = AppColor.TextMuted) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = AppColor.TextPrimary, unfocusedTextColor = AppColor.TextPrimary,
                cursorColor = AppColor.Accent, focusedBorderColor = AppColor.Accent, unfocusedBorderColor = AppColor.CardBorder,
                focusedContainerColor = AppColor.CardBackground, unfocusedContainerColor = AppColor.CardBackground
            ),
            shape = RoundedCornerShape(16.dp), singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 18.sp)
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.padding(24.dp)) { GradientButton("Continue", onContinue, enabled = name.isNotBlank()) }
    }
}

// ---- GENDER ----

@Composable
private fun GenderScreen(selected: String, onSelect: (String) -> Unit, onBack: () -> Unit, onContinue: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(AppColor.Background)) {
        OnboardingHeader("Choose your gender", "This helps us calibrate your custom plan.", 2f / TOTAL_STEPS, 2, TOTAL_STEPS, onBack)
        Spacer(modifier = Modifier.height(40.dp))
        Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SelectionOption("Male", emoji = "\uD83E\uDDD4", isSelected = selected == "Male", onClick = { onSelect("Male") })
            SelectionOption("Female", emoji = "\uD83D\uDC69", isSelected = selected == "Female", onClick = { onSelect("Female") })
            SelectionOption("Prefer not to say", emoji = "\uD83D\uDE42", isSelected = selected == "Other", onClick = { onSelect("Other") })
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.padding(24.dp)) { GradientButton("Continue", onContinue, enabled = selected.isNotEmpty()) }
    }
}

// ---- BODY STATS ----

@Composable
private fun BodyStatsScreen(
    age: String, height: String, weight: String,
    onAgeChange: (String) -> Unit, onHeightChange: (String) -> Unit, onWeightChange: (String) -> Unit,
    onBack: () -> Unit, onContinue: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(AppColor.Background).verticalScroll(rememberScrollState())) {
        OnboardingHeader("Your body stats", "Used to calculate personalized workout intensity.", 3f / TOTAL_STEPS, 3, TOTAL_STEPS, onBack)
        Spacer(modifier = Modifier.height(40.dp))
        Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            StatsTextField(age, { onAgeChange(it.filter { c -> c.isDigit() }.take(3)) }, "Age", "years")
            StatsTextField(height, { onHeightChange(it.filter { c -> c.isDigit() }.take(3)) }, "Height", "cm")
            StatsTextField(weight, { onWeightChange(it.filter { c -> c.isDigit() }.take(3)) }, "Weight", "kg")
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.padding(24.dp)) {
            GradientButton("Continue", onContinue, enabled = age.isNotBlank() && height.isNotBlank() && weight.isNotBlank())
        }
    }
}

@Composable
private fun StatsTextField(value: String, onValueChange: (String) -> Unit, label: String, suffix: String) {
    Column {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColor.TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(
            value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("0", color = AppColor.TextMuted) },
            suffix = { Text(suffix, color = AppColor.TextMuted, fontSize = 14.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = AppColor.TextPrimary, unfocusedTextColor = AppColor.TextPrimary,
                cursorColor = AppColor.Accent, focusedBorderColor = AppColor.Accent, unfocusedBorderColor = AppColor.CardBorder,
                focusedContainerColor = AppColor.CardBackground, unfocusedContainerColor = AppColor.CardBackground
            ),
            shape = RoundedCornerShape(16.dp), singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        )
    }
}

// ---- FITNESS GOAL ----

@Composable
private fun FitnessGoalScreen(selected: String, onSelect: (String) -> Unit, onBack: () -> Unit, onContinue: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(AppColor.Background)) {
        OnboardingHeader("What's your\nfitness goal?", "We'll customize your plan around this.", 4f / TOTAL_STEPS, 4, TOTAL_STEPS, onBack)
        Spacer(modifier = Modifier.height(32.dp))
        Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SelectionOption("Build Muscle", subtitle = "Gain strength and size", emoji = "\uD83D\uDCAA", isSelected = selected == "build_muscle", onClick = { onSelect("build_muscle") })
            SelectionOption("Lose Weight", subtitle = "Burn fat and get lean", emoji = "\uD83D\uDD25", isSelected = selected == "lose_weight", onClick = { onSelect("lose_weight") })
            SelectionOption("Stay Active", subtitle = "Maintain fitness and health", emoji = "\uD83C\uDFC3", isSelected = selected == "stay_active", onClick = { onSelect("stay_active") })
            SelectionOption("Improve Health", subtitle = "Better mobility and wellbeing", emoji = "\u2764\uFE0F", isSelected = selected == "improve_health", onClick = { onSelect("improve_health") })
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.padding(24.dp)) { GradientButton("Continue", onContinue, enabled = selected.isNotEmpty()) }
    }
}

// ---- EXPERIENCE ----

@Composable
private fun ExperienceScreen(selected: String, onSelect: (String) -> Unit, onBack: () -> Unit, onContinue: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(AppColor.Background)) {
        OnboardingHeader("Experience level", "This helps us set the right difficulty.", 5f / TOTAL_STEPS, 5, TOTAL_STEPS, onBack)
        Spacer(modifier = Modifier.height(40.dp))
        Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SelectionOption("Beginner", subtitle = "New to fitness or returning after a break", emoji = "\uD83C\uDF31", isSelected = selected == "beginner", onClick = { onSelect("beginner") })
            SelectionOption("Intermediate", subtitle = "Regular exercise for 6+ months", emoji = "\u26A1", isSelected = selected == "intermediate", onClick = { onSelect("intermediate") })
            SelectionOption("Advanced", subtitle = "Training consistently for 2+ years", emoji = "\uD83C\uDFC6", isSelected = selected == "advanced", onClick = { onSelect("advanced") })
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.padding(24.dp)) { GradientButton("Continue", onContinue, enabled = selected.isNotEmpty()) }
    }
}

// ---- WORKOUTS PER WEEK ----

@Composable
private fun WorkoutsScreen(selected: String, onSelect: (String) -> Unit, onBack: () -> Unit, onContinue: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(AppColor.Background)) {
        OnboardingHeader("How many workouts\nper week?", "We'll build your plan around your schedule.", 6f / TOTAL_STEPS, 6, TOTAL_STEPS, onBack)
        Spacer(modifier = Modifier.height(40.dp))
        Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SelectionOption("0-2 times", subtitle = "Just getting started", emoji = "\uD83D\uDEB6", isSelected = selected == "0-2", onClick = { onSelect("0-2") })
            SelectionOption("3-5 times", subtitle = "Consistent routine", emoji = "\uD83C\uDFCB\uFE0F", isSelected = selected == "3-5", onClick = { onSelect("3-5") })
            SelectionOption("6+ times", subtitle = "Dedicated athlete", emoji = "\uD83D\uDD25", isSelected = selected == "6+", onClick = { onSelect("6+") })
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.padding(24.dp)) { GradientButton("Continue", onContinue, enabled = selected.isNotEmpty()) }
    }
}

// ---- MIRROR PHOTO ----

@Composable
private fun MirrorPhotoScreen(photoPath: String?, onPhotoTaken: (String) -> Unit, onBack: () -> Unit, onContinue: () -> Unit) {
    val context = LocalContext.current
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(photoPath) {
        if (photoPath != null && capturedBitmap == null) {
            capturedBitmap = BitmapFactory.decodeFile(photoPath)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempPhotoUri != null) {
            val file = getPhotoFile(context)
            capturedBitmap = BitmapFactory.decodeFile(file.absolutePath)
            onPhotoTaken(file.absolutePath)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColor.Background)) {
        OnboardingHeader(
            "Take a mirror selfie",
            "This photo helps your AI coach generate personalized exercise guides showing YOU doing the workout correctly.",
            7f / TOTAL_STEPS, 7, TOTAL_STEPS, onBack
        )
        Spacer(modifier = Modifier.height(32.dp))
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).aspectRatio(0.75f)
                .clip(RoundedCornerShape(24.dp)).background(AppColor.CardBackground)
                .border(2.dp, AppColor.CardBorder, RoundedCornerShape(24.dp))
                .clickable {
                    val file = getPhotoFile(context)
                    tempPhotoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    cameraLauncher.launch(tempPhotoUri!!)
                },
            contentAlignment = Alignment.Center
        ) {
            if (capturedBitmap != null) {
                Image(
                    bitmap = capturedBitmap!!.asImageBitmap(),
                    contentDescription = "Mirror photo",
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                        .clip(CircleShape).background(AppColor.Background.copy(alpha = 0.7f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("Re-take", color = AppColor.Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(80.dp).clip(CircleShape)
                            .background(AppColor.Accent.copy(alpha = 0.15f))
                            .border(2.dp, AppColor.Accent.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text("\uD83D\uDCF8", fontSize = 36.sp) }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Tap to take photo", fontSize = 16.sp, color = AppColor.TextSecondary, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Full body in the mirror", fontSize = 13.sp, color = AppColor.TextMuted)
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.padding(24.dp)) {
            Column {
                GradientButton("Continue", onContinue, enabled = true)
                if (photoPath == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("You can skip this - add it later in settings", fontSize = 13.sp, color = AppColor.TextMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

private fun getPhotoFile(context: Context): File {
    val dir = File(context.filesDir, "photos")
    dir.mkdirs()
    return File(dir, "mirror_selfie.jpg")
}

// ---- CHART ----

@Composable
private fun ChartScreen(onBack: () -> Unit, onContinue: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(AppColor.Background)) {
        OnboardingHeader("Gymbro creates\nlong-term results", "", 8f / TOTAL_STEPS, 8, TOTAL_STEPS, onBack)
        Spacer(modifier = Modifier.height(48.dp))
        Box(
            modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)).background(AppColor.CardBackground)
                .border(1.dp, AppColor.CardBorder, RoundedCornerShape(24.dp)).padding(24.dp)
        ) {
            Column {
                Text("Your progress", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColor.TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(AppColor.Accent))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gymbro users", fontSize = 12.sp, color = AppColor.TextSecondary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(AppColor.Error.copy(alpha = 0.6f)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Traditional apps", fontSize = 12.sp, color = AppColor.TextSecondary)
                }
                Spacer(modifier = Modifier.height(24.dp))
                ProgressChartCanvas()
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Month 1", fontSize = 12.sp, color = AppColor.TextMuted)
                    Text("Month 6", fontSize = 12.sp, color = AppColor.TextMuted)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("80% of Gymbro users maintain their fitness goals even 6 months later", fontSize = 15.sp, color = AppColor.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), lineHeight = 22.sp)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.padding(24.dp)) { GradientButton("Continue", onContinue) }
    }
}

@Composable
private fun ProgressChartCanvas() {
    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        val width = size.width
        val height = size.height
        for (y in listOf(0.25f, 0.5f, 0.75f)) {
            drawLine(Color(0xFF2A2A3A), Offset(0f, height * y), Offset(width, height * y), strokeWidth = 1f)
        }
        val tradPath = Path().apply {
            moveTo(0f, height * 0.25f)
            cubicTo(width * 0.3f, height * 0.25f, width * 0.5f, height * 0.8f, width, height * 0.1f)
        }
        drawPath(tradPath, Color(0xFFFF5252).copy(alpha = 0.5f), style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val gymbroPath = Path().apply {
            moveTo(0f, height * 0.25f)
            cubicTo(width * 0.4f, height * 0.3f, width * 0.6f, height * 0.85f, width, height * 0.92f)
        }
        drawPath(gymbroPath, Color(0xFF00E676), style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(gymbroPath, Color(0xFF00E676).copy(alpha = 0.2f), style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(Color(0xFF00E676), radius = 7f, center = Offset(width, height * 0.92f))
        drawCircle(Color(0xFF0A0A0A), radius = 4f, center = Offset(width, height * 0.92f))
    }
}

// ---- ALL SET ----

@Composable
private fun AllSetScreen(name: String, onFinish: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(AppColor.Background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier.size(120.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(AppColor.Accent.copy(alpha = 0.3f), AppColor.Accent.copy(alpha = 0.05f)))),
            contentAlignment = Alignment.Center
        ) { Text("\u2705", fontSize = 56.sp) }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = if (name.isNotBlank()) "You're all set, $name!" else "You're all set!",
            fontSize = 32.sp, fontWeight = FontWeight.Black, color = AppColor.TextPrimary, textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("Your AI gym coach is ready.\nConnect your glasses and let's go.", fontSize = 16.sp, color = AppColor.TextSecondary, textAlign = TextAlign.Center, lineHeight = 24.sp)
        Spacer(modifier = Modifier.weight(1f))
        GradientButton("Let's Go \uD83C\uDFCB\uFE0F", onFinish)
        Spacer(modifier = Modifier.height(32.dp))
    }
}
