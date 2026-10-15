package com.shkurta.pickle.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.svg.SvgDecoder
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shkurta.pickle.R
import com.shkurta.pickle.ui.theme.Pickle
import com.shkurta.pickle.ui.theme.PickleTheme

val PickleCardShape = RoundedCornerShape(16.dp)
val PickleBorderWidth = 1.5.dp

// ---------------------------------------------------------------- Buttons

enum class PickleButtonStyle { Primary, Teal, Tomato, Outline, Ghost }

@Composable
fun PickleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PickleButtonStyle = PickleButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: Int? = null,
    height: Dp = 52.dp,
) {
    val c = Pickle.colors
    val (container, content) = when (style) {
        PickleButtonStyle.Primary -> c.ink to c.inkInverse
        PickleButtonStyle.Teal -> c.teal to c.onBrand
        PickleButtonStyle.Tomato -> c.tomato to c.onBrand
        PickleButtonStyle.Outline -> c.surface to c.ink
        PickleButtonStyle.Ghost -> Color.Transparent to c.ink
    }
    val border = when (style) {
        PickleButtonStyle.Ghost -> null
        else -> BorderStroke(PickleBorderWidth, if (enabled) c.lineStrong else c.lineSoft)
    }

    Button(
        onClick = onClick,
        enabled = enabled && !loading && LocalActionsEnabled.current,
        modifier = modifier.heightIn(min = height),
        shape = CircleShape,
        border = border,
        contentPadding = PaddingValues(horizontal = 20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = if (style == PickleButtonStyle.Ghost) Color.Transparent else c.sunken,
            disabledContentColor = c.inkMuted,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = content,
                strokeWidth = 2.dp,
            )
        } else {
            if (leadingIcon != null) {
                Icon(
                    painter = painterResource(leadingIcon),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** Round outlined icon button (back, close, overflow). */
@Composable
fun CircleIconButton(
    iconRes: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = Pickle.colors
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(40.dp),
        shape = CircleShape,
        color = c.surface,
        contentColor = c.ink,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** Dashed teal call-to-action, e.g. "+ Add an option" / "+ Suggest an option". */
@Composable
fun DashedActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = Pickle.colors
    val actionsEnabled = enabled && LocalActionsEnabled.current
    val color = if (actionsEnabled) c.teal else c.inkMuted
    Surface(
        onClick = onClick,
        enabled = actionsEnabled,
        modifier = modifier
            .fillMaxWidth()
            .dashedBorder(color, PickleCardShapeRadius),
        color = Color.Transparent,
        contentColor = color,
        shape = PickleCardShape,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private val PickleCardShapeRadius = 16.dp

fun Modifier.dashedBorder(color: Color, radius: Dp, width: Dp = PickleBorderWidth): Modifier =
    drawWithContent {
        drawContent()
        val strokePx = width.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(strokePx / 2, strokePx / 2),
            size = size.copy(width = size.width - strokePx, height = size.height - strokePx),
            cornerRadius = CornerRadius(radius.toPx()),
            style = Stroke(
                width = strokePx,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx())),
            ),
        )
    }

// ---------------------------------------------------------------- Cards

/** Cream card with the ink outline used throughout the design. */
@Composable
fun PickleCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = Pickle.colors.surface,
    borderColor: Color = Pickle.colors.lineStrong,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val border = BorderStroke(PickleBorderWidth, borderColor)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            enabled = LocalActionsEnabled.current,
            modifier = modifier.fillMaxWidth(),
            color = color,
            contentColor = Pickle.colors.ink,
            shape = PickleCardShape,
            border = border,
        ) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = color,
            contentColor = Pickle.colors.ink,
            shape = PickleCardShape,
            border = border,
        ) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

// ---------------------------------------------------------------- Text

/** Chunky heading with the mustard "sticker" offset shadow. */
@Composable
fun PickleHeadline(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    textAlign: TextAlign? = null,
) {

    Text(
        text = text,
        modifier = modifier,
        style = style.copy(
            color = Pickle.colors.ink,
        ),
        textAlign = textAlign,
    )
}

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = Pickle.colors.inkMuted,
        modifier = modifier.padding(start = 2.dp, top = 16.dp, bottom = 4.dp),
    )
}

// ---------------------------------------------------------------- Pills

/** "● Voting open" (teal-soft) / "● Voting closed" (outlined). */
@Composable
fun StatusPill(
    isOpen: Boolean,
    modifier: Modifier = Modifier,
    openLabel: String = "Voting open",
    closedLabel: String = "Voting closed",
) {
    val c = Pickle.colors
    Row(
        modifier = modifier
            .background(if (isOpen) c.tealSoft else c.surface, CircleShape)
            .then(
                if (isOpen) Modifier
                else Modifier.border(PickleBorderWidth, c.lineStrong, CircleShape)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(7.dp)
                .background(if (isOpen) c.teal else c.ink, CircleShape)
        )
        Text(
            text = if (isOpen) openLabel else closedLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isOpen) c.teal else c.ink,
        )
    }
}

/** Outlined pill with an optional icon, e.g. "◷ Closes today at 7:00 PM". */
@Composable
fun InfoPill(
    text: String,
    modifier: Modifier = Modifier,
    icon: Int? = null,
    container: Color = Pickle.colors.surface,
    content: Color = Pickle.colors.ink,
    borderColor: Color? = Pickle.colors.lineStrong,
) {
    Row(
        modifier = modifier
            .background(container, CircleShape)
            .then(
                if (borderColor != null) Modifier.border(PickleBorderWidth, borderColor, CircleShape)
                else Modifier
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = content,
        )
    }
}

// ---------------------------------------------------------------- Brand

/** Tomato check mark + wordmark, top-left of the main screens. */
@Composable
fun PickleBrand(
    modifier: Modifier = Modifier,
) {
    val c = Pickle.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(c.tomato, CircleShape)
                .border(PickleBorderWidth, c.lineStrong, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = c.onBrand,
                modifier = Modifier.size(13.dp),
            )
        }
        Text(
            text = "pickle",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = c.ink,
        )
    }
}

/** Round DiceBear face with an ink outline. Falls back to an initial when there's no URL. */
@Composable
fun Avatar(
    url: String?,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    fallbackInitial: String? = null,
) {
    val c = Pickle.colors
    val context = LocalContext.current
    val avatarModifier = modifier
        .size(size)
        .clip(CircleShape)
        .background(c.tealSoft)
        .border(PickleBorderWidth, c.lineStrong, CircleShape)
    if (url != null) {
        AsyncImage(
            model = remember(url) {
                ImageRequest.Builder(context)
                    .data(url)
                    .decoderFactory(SvgDecoder.Factory())
                    .build()
            },
            contentDescription = null,
            modifier = avatarModifier,
        )
    } else {
        Box(avatarModifier, contentAlignment = Alignment.Center) {
            Text(
                text = fallbackInitial?.take(1)?.uppercase().orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                color = c.ink,
            )
        }
    }
}

// ---------------------------------------------------------------- Controls

@Composable
fun pickleSwitchColors(): SwitchColors {
    val c = Pickle.colors
    return SwitchDefaults.colors(
        checkedThumbColor = c.surface,
        checkedTrackColor = c.teal,
        checkedBorderColor = c.lineStrong,
        uncheckedThumbColor = c.inkMuted,
        uncheckedTrackColor = c.sunken,
        uncheckedBorderColor = c.lineStrong,
    )
}

/** Segmented results bar: [filled] blocks in [color], the rest on the sunken track. */
@Composable
fun BlockMeter(
    filled: Int,
    total: Int,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    blockHeight: Dp = 8.dp,
) {
    if (total <= 0) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(blockHeight)
                    .background(if (index < filled) color else trackColor, CircleShape),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6EFE3)
@Composable
private fun PickleComponentsPreview() {
    PickleTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PickleBrand()
            PickleHeadline("Can't decide?")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(isOpen = true)
                StatusPill(isOpen = false)
            }
            PickleButton("Primary", {}, Modifier.fillMaxWidth())
            PickleButton("Teal", {}, Modifier.fillMaxWidth(), style = PickleButtonStyle.Teal)
            PickleButton("Tomato", {}, Modifier.fillMaxWidth(), style = PickleButtonStyle.Tomato)
            PickleButton("Outline", {}, Modifier.fillMaxWidth(), style = PickleButtonStyle.Outline)
            DashedActionButton("Add an option", {})
            BlockMeter(5, 11, Pickle.colors.mustard, Pickle.colors.sunken)
        }
    }
}
