package com.shkurta.pickle.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.shkurta.pickle.data.sample.SamplePolls
import com.shkurta.pickle.domain.model.Poll
import com.shkurta.pickle.ui.theme.PickleTheme
import java.time.Duration
import java.time.Instant

internal val ClosedColor = Color(0xFF8C7A60)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PollCard(
    poll: Poll,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
) {
    val isOpen = poll.isOpen()
    val hasVotes = poll.totalVotes > 0
    val cardShape = RoundedCornerShape(20.dp)
    val borderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f)

    val dashedBorderModifier = if (!hasVotes) {
        Modifier.drawWithContent {
            drawContent()
            val stroke = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(10.dp.toPx(), 6.dp.toPx()),
                ),
            )
            drawRoundRect(
                color = borderColor,
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(20.dp.toPx()),
                style = stroke,
            )
        }
    } else {
        Modifier
    }

    Surface(
        onClick = onClick,
        modifier = modifier.then(dashedBorderModifier),
        color = MaterialTheme.colorScheme.surface,
        shape = cardShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusChip(isOpen = isOpen)

                val timeLabel = timeLabel(poll)
                if (timeLabel != null) {
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = poll.question,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(6.dp))

            val optionPreview = poll.approvedOptions
                .sortedByDescending { it.voteCount }
                .take(3)
                .joinToString(" · ") { it.text }
            if (optionPreview.isNotEmpty()) {
                Text(
                    text = optionPreview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (hasVotes) {
                val turnout = poll.turnout
                if (turnout != null) {
                    Spacer(Modifier.height(16.dp))

                    val animatedProgress by animateFloatAsState(
                        targetValue = turnout,
                        animationSpec = tween(600),
                        label = "turnout",
                    )

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isOpen) MaterialTheme.colorScheme.primary else ClosedColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round,
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = voteSummary(poll),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onShareClick,
                ) {
                    Text(
                        text = "Share link",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
internal fun StatusChip(
    isOpen: Boolean,
    modifier: Modifier = Modifier,
) {
    val chipColor = if (isOpen) {
        MaterialTheme.colorScheme.primary
    } else {
        ClosedColor
    }

    Surface(
        modifier = modifier,
        color = chipColor.copy(alpha = 0.10f),
        shape = RoundedCornerShape(200.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(8.dp),
                shape = CircleShape,
                color = chipColor,
                content = {},
            )
            Text(
                text = if (isOpen) "Open" else "Closed",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = chipColor,
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun timeLabel(poll: Poll, now: Instant = Instant.now()): String? {
    val closedAt = poll.closedAt ?: return null
    val duration = if (poll.isOpen(now)) {
        Duration.between(now, closedAt)
    } else {
        Duration.between(closedAt, now)
    }

    val label = when {
        duration.toDays() > 0 -> "${duration.toDays()}d"
        duration.toHours() > 0 -> "${duration.toHours()}h"
        duration.toMinutes() > 0 -> "${duration.toMinutes()}m"
        else -> "<1m"
    }

    return if (poll.isOpen(now)) "Ends in $label" else "Closed $label ago"
}

internal fun voteSummary(poll: Poll): String {
    val votes = poll.totalVotes
    return when {
        poll.expectedVoters != null -> "$votes of ${poll.expectedVoters} voted"
        votes == 0 -> "No votes yet"
        votes == 1 -> "1 vote"
        else -> "$votes votes"
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, backgroundColor = 0xFFFAF2E3)
@Composable
private fun PollCardPreview(
    @PreviewParameter(SamplePollProvider::class) poll: Poll,
) {
    PickleTheme {
        PollCard(
            poll = poll,
            modifier = Modifier.padding(16.dp),
        )
    }
}

class SamplePollProvider : PreviewParameterProvider<Poll> {
    @RequiresApi(Build.VERSION_CODES.O)
    override val values = SamplePolls.all.asSequence()
}