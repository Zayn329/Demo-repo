package org.sahara.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// =============================================================================
// PRIMARY & SECONDARY BUTTONS
// =============================================================================

@Composable
fun SaharaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDanger: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = if (enabled) 6.dp else 0.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = if (isDanger) SaharaColors.DangerCoral else SaharaColors.PinkPrimary,
                ambientColor = SaharaColors.ShadowTint
            ),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isDanger) SaharaColors.DangerCoral else SaharaColors.PinkPrimary,
            contentColor = SaharaColors.TextInverse,
            disabledContainerColor = SaharaColors.SoftPink,
            disabledContentColor = SaharaColors.TextDisabled
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SaharaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = Brush.horizontalGradient(
                listOf(SaharaColors.BorderSubtle, SaharaColors.BorderSubtle)
            )
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SaharaColors.PureWhite,
            contentColor = SaharaColors.TextPrimary
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// =============================================================================
// STATUS BADGES & PILLS
// =============================================================================

enum class BadgeStyle {
    SUCCESS,
    INFO,
    WARNING,
    NEUTRAL,
    ACTIVE_PINK
}

@Composable
fun SaharaStatusBadge(
    text: String,
    style: BadgeStyle = BadgeStyle.NEUTRAL
) {
    val (bgColor, textColor) = when (style) {
        BadgeStyle.SUCCESS -> Pair(SaharaColors.SuccessGreenBg, SaharaColors.SuccessGreen)
        BadgeStyle.INFO -> Pair(SaharaColors.VerySoftBlue, SaharaColors.BlueMuted)
        BadgeStyle.WARNING -> Pair(SaharaColors.VerySoftYellow, SaharaColors.YellowDark)
        BadgeStyle.ACTIVE_PINK -> Pair(SaharaColors.SoftPink, SaharaColors.PinkDeep)
        BadgeStyle.NEUTRAL -> Pair(SaharaColors.SurfaceSubtle, SaharaColors.TextSecondary)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

// =============================================================================
// TOGGLE CARD (Preference Items)
// =============================================================================

@Composable
fun SaharaToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = SaharaColors.PinkPrimary
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = SaharaColors.ShadowTint,
                ambientColor = SaharaColors.ShadowTint
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SaharaColors.PureWhite)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SaharaColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = SaharaColors.TextSecondary
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SaharaColors.PureWhite,
                    checkedTrackColor = accentColor,
                    uncheckedThumbColor = SaharaColors.PureWhite,
                    uncheckedTrackColor = SaharaColors.BorderSubtle,
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    }
}

// =============================================================================
// SAFETY STATUS CARD (Dashboard & Status)
// =============================================================================

@Composable
fun SaharaSafetyStatusCard(
    title: String,
    subtitle: String,
    stateText: String,
    modifier: Modifier = Modifier,
    stateBadgeStyle: BadgeStyle = BadgeStyle.SUCCESS,
    containerColor: Color = SaharaColors.PureWhite,
    iconLetter: String = "✓"
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = SaharaColors.ShadowTint,
                ambientColor = SaharaColors.ShadowTint
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SaharaColors.VerySoftPink),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = iconLetter,
                        color = SaharaColors.PinkPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SaharaColors.TextPrimary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = SaharaColors.TextSecondary
                    )
                }
            }
            SaharaStatusBadge(text = stateText, style = stateBadgeStyle)
        }
    }
}

// =============================================================================
// CONTACT CARD
// =============================================================================

@Composable
fun SaharaContactCard(
    name: String,
    relation: String,
    status: String,
    modifier: Modifier = Modifier,
    avatarColor: Color = SaharaColors.SoftPink,
    onRemove: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = SaharaColors.ShadowTint,
                ambientColor = SaharaColors.ShadowTint
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SaharaColors.PureWhite)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.take(1).uppercase(),
                        color = SaharaColors.PinkPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SaharaColors.TextPrimary
                    )
                    Text(
                        text = relation,
                        style = MaterialTheme.typography.bodySmall,
                        color = SaharaColors.TextSecondary
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                SaharaStatusBadge(text = status, style = BadgeStyle.SUCCESS)
                if (onRemove != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "✕",
                        color = SaharaColors.TextDisabled,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onRemove() }
                            .padding(6.dp)
                    )
                }
            }
        }
    }
}

// =============================================================================
// TIMELINE ITEM
// =============================================================================

@Composable
fun SaharaTimelineItem(
    time: String,
    title: String,
    subtitle: String? = null,
    isLast: Boolean = false,
    isVerified: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        // Time Column
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = SaharaColors.TextSecondary,
            modifier = Modifier.width(64.dp)
        )

        // Indicator Line Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (isVerified) SaharaColors.SuccessGreen else SaharaColors.PinkPrimary),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(SaharaColors.PureWhite)
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(SaharaColors.BorderSubtle)
                )
            }
        }

        // Event Details
        Column(modifier = Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = SaharaColors.TextPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SaharaColors.TextSecondary
                )
            }
        }
    }
}

// =============================================================================
// SECTION HEADER
// =============================================================================

@Composable
fun SaharaSectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = SaharaColors.TextPrimary
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = SaharaColors.TextSecondary
            )
        }
    }
}

// =============================================================================
// PRESS AND HOLD INTERACTION BUTTON
// =============================================================================

@Composable
fun SaharaHoldToActivateButton(
    text: String,
    subtext: String = "Press & hold to activate",
    onHoldComplete: () -> Unit,
    modifier: Modifier = Modifier,
    isDanger: Boolean = false,
    holdDurationMs: Long = 1800L
) {
    var progress by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }

    LaunchedEffect(isHolding) {
        if (isHolding) {
            val stepTime = 30L
            val totalSteps = holdDurationMs / stepTime
            for (i in 1..totalSteps) {
                delay(stepTime)
                if (!isHolding) {
                    progress = 0f
                    break
                }
                progress = i / totalSteps.toFloat()
            }
            if (progress >= 1f) {
                onHoldComplete()
                progress = 0f
                isHolding = false
            }
        } else {
            progress = 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(32.dp),
                spotColor = if (isDanger) SaharaColors.DangerCoral else SaharaColors.PinkPrimary,
                ambientColor = SaharaColors.ShadowTint
            )
            .clip(RoundedCornerShape(32.dp))
            .background(if (isDanger) SaharaColors.DangerCoral else SaharaColors.PinkPrimary)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isHolding = true
                        tryAwaitRelease()
                        isHolding = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Progress Fill
        if (progress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(64.dp)
                    .align(Alignment.CenterStart)
                    .background(
                        if (isDanger) Color(0xFFB71C1C).copy(alpha = 0.4f)
                        else SaharaColors.PinkDeep.copy(alpha = 0.4f)
                    )
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SaharaColors.TextInverse
            )
            Text(
                text = if (isHolding) "Keep holding..." else subtext,
                style = MaterialTheme.typography.labelSmall,
                color = SaharaColors.TextInverse.copy(alpha = 0.85f)
            )
        }
    }
}

// =============================================================================
// BREATHING CALM SAFETY VISUAL (STAR SCREEN)
// =============================================================================

@Composable
fun BreathingSafetyVisual(
    statusText: String = "Safety Agent active",
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .size(200.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing aura
        Box(
            modifier = Modifier
                .size(170.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(SaharaColors.VerySoftPink.copy(alpha = alphaPulse))
        )

        // Middle soft blue aura
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(SaharaColors.SoftPink, SaharaColors.VerySoftBlue)
                    )
                )
        )

        // Core calm pearl
        Box(
            modifier = Modifier
                .size(90.dp)
                .shadow(6.dp, CircleShape, spotColor = SaharaColors.PinkPrimary)
                .clip(CircleShape)
                .background(SaharaColors.PureWhite),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(SaharaColors.PinkPrimary)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "SAFE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = SaharaColors.PinkPrimary,
                    letterSpacing = 1.sp
                )
                if (statusText.isNotBlank()) {
                    Text(
                        text = "●",
                        fontSize = 6.sp,
                        color = SaharaColors.SuccessGreen
                    )
                }
            }
        }
    }
}

// =============================================================================
// BOTTOM NAVIGATION
// =============================================================================

@Composable
fun SaharaBottomNav(
    selectedTab: String,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        Pair("Home", "🏠"),
        Pair("Circle", "👥"),
        Pair("Records", "📋"),
        Pair("Settings", "⚙️")
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = SaharaColors.PureWhite)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { (tabName, iconEmoji) ->
                val isSelected = selectedTab == tabName
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectTab(tabName) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = iconEmoji,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tabName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) SaharaColors.PinkPrimary else SaharaColors.TextSecondary
                    )
                }
            }
        }
    }
}
