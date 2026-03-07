package com.meta.wearable.dat.externalsampleapps.cameraaccess.health.screens.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.HealthUiState
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.components.MetricCard
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.data.*
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthColors
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthSpacing
import com.meta.wearable.dat.externalsampleapps.cameraaccess.health.theme.HealthTypography

@Composable
fun DashboardScreen(
    uiState: HealthUiState,
    onNavigateToCommunity: () -> Unit,
    onAddMeal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val user = uiState.user
    val nutrition = uiState.nutrition
    val posts = uiState.communityPosts

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HealthColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(HealthSpacing.screenPadding)
        ) {
            // 1. Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(HealthColors.Accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.first().toString(),
                        color = Color.Black,
                        fontSize = HealthTypography.sectionHeaderSize,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Good Morning!",
                        color = HealthColors.TextSecondary,
                        fontSize = HealthTypography.bodySize
                    )
                    Text(
                        text = user.name,
                        color = HealthColors.TextPrimary,
                        fontSize = HealthTypography.sectionHeaderSize,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "Notifications",
                        tint = HealthColors.TextSecondary
                    )
                }
            }

            // 2. Spacer
            Spacer(modifier = Modifier.height(24.dp))

            // 3. Daily Nutrition section title
            Text(
                text = "Your Daily Nutrition",
                color = HealthColors.TextPrimary,
                fontSize = HealthTypography.sectionHeaderSize,
                fontWeight = FontWeight.SemiBold
            )

            // 4. Spacer
            Spacer(modifier = Modifier.height(12.dp))

            // 5. Row of 3 MetricCards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    icon = Icons.Filled.LocalFireDepartment,
                    value = nutrition.totalCalories.toString(),
                    unit = "kcal",
                    label = "of ${nutrition.goalCalories}",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    icon = Icons.Filled.FitnessCenter,
                    value = nutrition.proteins.current.toString(),
                    unit = "g",
                    label = "of ${nutrition.proteins.goal}g",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    icon = Icons.Filled.Restaurant,
                    value = nutrition.carbs.current.toString(),
                    unit = "g",
                    label = "of ${nutrition.carbs.goal}g",
                    modifier = Modifier.weight(1f)
                )
            }

            // 6. Spacer
            Spacer(modifier = Modifier.height(24.dp))

            // 7. Cooking Community section title with "See All"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cooking Community",
                    color = HealthColors.TextPrimary,
                    fontSize = HealthTypography.sectionHeaderSize,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "See All",
                    color = HealthColors.Accent,
                    fontSize = HealthTypography.bodySize,
                    modifier = Modifier.clickable { onNavigateToCommunity() }
                )
            }

            // 8. Spacer
            Spacer(modifier = Modifier.height(12.dp))

            // 9. Community preview card
            if (posts.isNotEmpty()) {
                val post = posts.first()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(HealthSpacing.cardRadius),
                    colors = CardDefaults.cardColors(containerColor = HealthColors.Card)
                ) {
                    Column(modifier = Modifier.padding(HealthSpacing.md)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(post.avatarColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = post.author.first().toString(),
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Text(
                                    text = post.author,
                                    color = HealthColors.TextPrimary,
                                    fontSize = HealthTypography.bodySize,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = post.timestamp,
                                    color = HealthColors.TextSecondary,
                                    fontSize = HealthTypography.subLabelSize
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            OutlinedButton(
                                onClick = { },
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(HealthColors.Accent)
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = HealthColors.Accent
                                )
                            ) {
                                Text(text = "Follow")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = post.text,
                            color = HealthColors.TextPrimary,
                            fontSize = HealthTypography.bodySize
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Engagement row
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Likes",
                                tint = HealthColors.Heart,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = post.likes.toString(),
                                color = HealthColors.TextSecondary,
                                fontSize = HealthTypography.subLabelSize
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(
                                imageVector = Icons.Filled.ChatBubbleOutline,
                                contentDescription = "Comments",
                                tint = HealthColors.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = post.comments.toString(),
                                color = HealthColors.TextSecondary,
                                fontSize = HealthTypography.subLabelSize
                            )
                        }
                    }
                }
            }

            // 10. Spacer for FAB space
            Spacer(modifier = Modifier.height(100.dp))
        }

        // 11. FloatingActionButton at bottom-center
        FloatingActionButton(
            onClick = onAddMeal,
            containerColor = HealthColors.Accent,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Meal",
                tint = Color.Black
            )
        }
    }
}
