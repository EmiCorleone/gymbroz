package com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.workout

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.ExerciseGuideUiState
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.data.Workout
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthColors

@Composable
fun ActiveWorkoutScreen(
    workout: Workout,
    exerciseGuides: Map<String, ExerciseGuideUiState> = emptyMap(),
    onComplete: () -> Unit,
    onDismiss: () -> Unit = onComplete,
    onRequestGuide: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (workout.exercises.isEmpty()) return

    var currentExerciseIndex by remember { mutableIntStateOf(0) }
    var currentSet by remember { mutableIntStateOf(1) }
    var expandedImage by remember { mutableStateOf<String?>(null) } // base64 of image to show fullscreen

    val currentExercise = workout.exercises[currentExerciseIndex]
    val totalExercises = workout.exercises.size
    val guide = exerciseGuides[currentExercise.name]

    // Auto-request guide generation for exercises without images
    LaunchedEffect(currentExercise.name) {
        if (guide == null && onRequestGuide != null) {
            Log.d("ActiveWorkoutScreen", "Requesting guide generation for '${currentExercise.name}'")
            onRequestGuide(currentExercise.name)
        }
    }

    LaunchedEffect(currentExerciseIndex) { currentSet = 1 }

    fun advanceExercise() {
        if (currentExerciseIndex < totalExercises - 1) {
            currentExerciseIndex++
        } else {
            onComplete()
        }
    }

    // Fullscreen image preview
    AnimatedVisibility(
        visible = expandedImage != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        expandedImage?.let { base64 ->
            val bitmap = remember(base64) {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .clickable { expandedImage = null },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Enlarged exercise guide",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                    // Close hint
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(20.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable { expandedImage = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    // Main overlay — only show when not in fullscreen image mode
    if (expandedImage == null) {
        Box(modifier = modifier.fillMaxSize()) {
            // Dismiss button — top right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White, modifier = Modifier.size(16.dp))
            }

            // Exercise overlay — positioned above the bottom buttons area
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 100.dp, start = 8.dp, end = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Images row with prev/next buttons on the sides
                if (guide != null && (guide.isGenerating || guide.imageBase64 != null || guide.closeUpImageBase64 != null)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Back/previous button — left side
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (currentExerciseIndex > 0 || currentSet > 1)
                                        Color.White.copy(alpha = 0.15f)
                                    else Color.White.copy(alpha = 0.05f)
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                .clickable(enabled = currentExerciseIndex > 0 || currentSet > 1) {
                                    if (currentSet > 1) currentSet--
                                    else if (currentExerciseIndex > 0) currentExerciseIndex--
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = if (currentExerciseIndex > 0 || currentSet > 1)
                                    Color.White else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Two guide images
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Full body — tappable
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .then(
                                        if (guide.imageBase64 != null) Modifier.clickable { expandedImage = guide.imageBase64 }
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    guide.imageBase64 != null -> {
                                        val bitmap = remember(guide.imageBase64) {
                                            val bytes = Base64.decode(guide.imageBase64, Base64.DEFAULT)
                                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                        }
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Your form",
                                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                    guide.isGenerating -> {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = HealthColors.Accent, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Generating...", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
                                        }
                                    }
                                }
                            }
                            // Close-up — tappable
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .then(
                                        if (guide.closeUpImageBase64 != null) Modifier.clickable { expandedImage = guide.closeUpImageBase64 }
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    guide.closeUpImageBase64 != null -> {
                                        val bitmap = remember(guide.closeUpImageBase64) {
                                            val bytes = Base64.decode(guide.closeUpImageBase64, Base64.DEFAULT)
                                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                        }
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Close-up form",
                                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                    guide.isGenerating -> {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = HealthColors.Accent, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Generating...", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Next button — right side
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(HealthColors.Accent)
                                .clickable {
                                    val totalSets = currentExercise.sets ?: 1
                                    if (currentSet < totalSets) currentSet++
                                    else advanceExercise()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (currentSet >= (currentExercise.sets ?: 1))
                                    Icons.Default.SkipNext else Icons.Default.PlayArrow,
                                contentDescription = "Next",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Exercise name + set info — compact glass pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .border(
                            1.dp,
                            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f))),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentExercise.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val detail = if (currentExercise.durationSec != null) {
                            "${currentExercise.durationSec}s"
                        } else {
                            "Set $currentSet/${currentExercise.sets ?: 1} · ${currentExercise.reps ?: 0} reps"
                        }
                        Text(text = detail, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }

                    // If no images showing, put back/next buttons here in the pill
                    if (guide == null || (!guide.isGenerating && guide.imageBase64 == null && guide.closeUpImageBase64 == null)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Back button
                            if (currentExerciseIndex > 0 || currentSet > 1) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f))
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                        .clickable {
                                            if (currentSet > 1) currentSet--
                                            else if (currentExerciseIndex > 0) currentExerciseIndex--
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                            // Next button
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(HealthColors.Accent)
                                    .clickable {
                                        val totalSets = currentExercise.sets ?: 1
                                        if (currentSet < totalSets) currentSet++
                                        else advanceExercise()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (currentSet >= (currentExercise.sets ?: 1))
                                        Icons.Default.SkipNext else Icons.Default.PlayArrow,
                                    contentDescription = "Next",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Progress dots — tappable to jump to exercise
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(totalExercises) { i ->
                        Box(
                            modifier = Modifier
                                .size(if (i == currentExerciseIndex) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i < currentExerciseIndex) HealthColors.Accent
                                    else if (i == currentExerciseIndex) Color.White
                                    else Color.White.copy(alpha = 0.3f)
                                )
                                .clickable { currentExerciseIndex = i }
                        )
                    }
                }
            }
        }
    }
}
