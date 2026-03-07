package com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.ui.AppColor

// =====================================================================
// GLASS CARD -- Frosted glass with gradient border, highlight & glow
// =====================================================================

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    glowColor: Color = AppColor.Accent,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.glassGlow(glowColor) else Modifier
            )
            .clip(shape)
            .glassBackground(
                alpha = if (isSelected) 0.14f else 0.08f,
                shape = shape,
            )
            .glassBorder(
                shape = shape,
                isSelected = isSelected,
                accentColor = glowColor,
            )
            .glassHighlight(cornerRadius = 24.dp)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ) else Modifier
            )
            .padding(20.dp)
    ) {
        content()
    }
}

// =====================================================================
// GRADIENT BUTTON -- Primary action with shimmer sweep
// =====================================================================

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: List<Color> = listOf(AppColor.Primary, Color(0xFF00BFA5), AppColor.Accent),
) {
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.4f,
        animationSpec = tween(300),
        label = "button_alpha",
    )

    val transition = rememberInfiniteTransition(label = "btn_shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "btn_shimmer_x",
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .alpha(alpha),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
        ),
        contentPadding = PaddingValues(0.dp),
        shape = CircleShape,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .background(
                    brush = Brush.horizontalGradient(colors),
                    shape = CircleShape,
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.2f),
                            Color.White.copy(alpha = 0.05f),
                            Color.White.copy(alpha = 0.15f),
                        )
                    ),
                    shape = CircleShape,
                )
                .drawBehind {
                    // Shimmer sweep
                    val sweepWidth = size.width * 0.35f
                    val x = size.width * shimmerX
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.18f),
                                Color.Transparent,
                            ),
                            start = Offset(x - sweepWidth, 0f),
                            end = Offset(x + sweepWidth, size.height),
                        ),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = AppColor.TextOnAccent,
                letterSpacing = 0.5.sp,
            )
        }
    }
}

// =====================================================================
// SELECTION CARD -- For onboarding options with glass treatment
// =====================================================================

@Composable
fun SelectionOption(
    title: String,
    subtitle: String? = null,
    emoji: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = tween(200),
        label = "selection_scale",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                if (isSelected) Modifier.glassGlow(AppColor.Accent, radiusFraction = 0.5f) else Modifier
            )
            .clip(shape)
            .glassBackground(
                alpha = if (isSelected) 0.14f else 0.06f,
                shape = shape,
            )
            .glassBorder(
                shape = shape,
                isSelected = isSelected,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (emoji != null) {
            Text(
                text = emoji,
                fontSize = 28.sp,
                modifier = Modifier.padding(end = 16.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) AppColor.TextPrimary else AppColor.TextSecondary,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = AppColor.TextMuted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        // Selection indicator — glass circle
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) {
                        Brush.radialGradient(
                            listOf(AppColor.Accent, AppColor.AccentDark),
                        )
                    } else {
                        Brush.radialGradient(
                            listOf(Color.Transparent, Color.Transparent),
                        )
                    }
                )
                .border(
                    2.dp,
                    if (isSelected) AppColor.Accent else AppColor.TextMuted.copy(alpha = 0.4f),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Text(
                    text = "\u2713",
                    fontSize = 14.sp,
                    color = AppColor.TextOnAccent,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// =====================================================================
// PULSE INDICATOR -- Animated pulsing dot with glow halo
// =====================================================================

@Composable
fun PulseIndicator(
    color: Color = AppColor.Accent,
    size: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Glow halo
        Box(
            modifier = Modifier
                .size(size * 2.5f)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = 0.2f), Color.Transparent),
                        ),
                    )
                }
        )
        // Outer pulse ring
        Box(
            modifier = Modifier
                .size(size * 1.8f)
                .scale(scale)
                .alpha(alpha)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.3f)),
        )
        // Inner solid dot
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color),
        )
    }
}

// =====================================================================
// STAT CARD -- For dashboard metrics (wraps GlassCard)
// =====================================================================

@Composable
fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accentColor: Color = AppColor.Accent,
) {
    GlassCard(modifier = modifier, glowColor = accentColor) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = accentColor,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AppColor.TextMuted,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp,
            )
        }
    }
}

// =====================================================================
// SECTION HEADER -- For labeled sections with gradient underline
// =====================================================================

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppColor.TextPrimary,
            )
            if (action != null && onAction != null) {
                Text(
                    text = action,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColor.Accent,
                    modifier = Modifier.clickable { onAction() },
                )
            }
        }
        // Gradient underline
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            AppColor.Accent.copy(alpha = 0.4f),
                            Color.Transparent,
                        )
                    )
                )
        )
    }
}
