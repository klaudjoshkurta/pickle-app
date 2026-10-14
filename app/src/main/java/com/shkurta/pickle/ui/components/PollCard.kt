package com.shkurta.pickle.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.shkurta.pickle.R
import com.shkurta.pickle.data.sample.SamplePolls
import com.shkurta.pickle.domain.model.Outcome
import com.shkurta.pickle.domain.model.Poll
import com.shkurta.pickle.domain.model.PollStatus
import com.shkurta.pickle.domain.model.toResults
import com.shkurta.pickle.ui.theme.Pickle
import com.shkurta.pickle.ui.theme.PickleColors
import com.shkurta.pickle.ui.theme.PickleTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PollCard(
    poll: Poll,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
) {
    val c = Pickle.colors
    val isOpen = poll.isOpen()
    val hasVotes = poll.totalVotes > 0
    val pending = poll.pendingSuggestions.size

    PickleCard(
        modifier = modifier,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusPill(isOpen = isOpen, openLabel = "Live", closedLabel = "Settled")
            val label = timeLabel(poll)
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = c.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = null,
                tint = c.ink,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(180f),
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = poll.question,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = c.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        if (hasVotes) {
            Spacer(Modifier.height(12.dp))
            StackedResultsBar(poll = poll)
            Spacer(Modifier.height(8.dp))
            Text(
                text = voteSummary(poll),
                style = MaterialTheme.typography.labelMedium,
                color = c.inkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else if (isOpen) {
            Spacer(Modifier.height(12.dp))
            PickleButton(
                text = "Share link",
                onClick = onShareClick,
                style = PickleButtonStyle.Teal,
                leadingIcon = R.drawable.ic_share,
                height = 40.dp,
            )
        } else {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Nobody voted",
                style = MaterialTheme.typography.labelMedium,
                color = c.inkMuted,
            )
        }

        if (isOpen && pending > 0) {
            Spacer(Modifier.height(10.dp))
            InfoPill(
                text = if (pending == 1) "1 suggestion to review" else "$pending suggestions to review",
                container = c.mustard,
                content = c.onMustard,
            )
        }
    }
}

/** One bar split by vote share: leader in tomato, then teal, mustard, muted ink. */
@Composable
fun StackedResultsBar(
    poll: Poll,
    modifier: Modifier = Modifier,
) {
    val c = Pickle.colors
    val options = poll.approvedOptions
        .filter { it.voteCount > 0 }
        .sortedByDescending { it.voteCount }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(c.sunken),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { index, option ->
            Box(
                modifier = Modifier
                    .weight(option.voteCount.toFloat())
                    .height(8.dp)
                    .background(c.seriesColor(index)),
            )
        }
    }
}

fun PickleColors.seriesColor(index: Int): Color = when (index) {
    0 -> tomato
    1 -> teal
    2 -> mustard
    else -> inkMuted
}

/** "Closes 7:00 PM", "Closes tomorrow 6:00 PM", "Settled Mon 6:41 PM". */
@RequiresApi(Build.VERSION_CODES.O)
internal fun timeLabel(poll: Poll, now: Instant = Instant.now()): String? {
    val closedAt = poll.closedAt ?: return null
    val prefix = if (poll.isOpen(now)) "Closes" else "Settled"
    return "$prefix ${friendlyTime(closedAt, now)}"
}

@RequiresApi(Build.VERSION_CODES.O)
internal fun friendlyTime(instant: Instant, now: Instant = Instant.now()): String {
    val zone = ZoneId.systemDefault()
    val date = instant.atZone(zone)
    val today = LocalDate.now(zone)
    val time = date.format(DateTimeFormatter.ofPattern("h:mm a"))
    return when (date.toLocalDate()) {
        today -> "today $time"
        today.plusDays(1) -> "tomorrow $time"
        today.minusDays(1) -> "yesterday $time"
        else -> "${date.format(DateTimeFormatter.ofPattern("EEE d MMM"))} $time"
    }
}

/** "11 votes · Detroit-style ahead by 2" */
internal fun voteSummary(poll: Poll): String {
    val votes = poll.totalVotes
    val count = when {
        poll.expectedVoters != null -> "$votes of ${poll.expectedVoters} voted"
        votes == 0 -> "No votes yet"
        votes == 1 -> "1 vote"
        else -> "$votes votes"
    }
    val results = poll.toResults()
    val lead = when (val outcome = results.outcome) {
        is Outcome.Winner -> {
            val margin = outcome.option.voteCount - (results.options.getOrNull(1)?.option?.voteCount ?: 0)
            val verb = if (poll.status == PollStatus.CLOSED) "won" else "ahead"
            "${outcome.option.text} $verb by $margin"
        }
        is Outcome.Tie -> "tied at ${outcome.options.first().voteCount}"
        Outcome.NoVotes -> null
    }
    return listOfNotNull(count, lead).joinToString(" · ")
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, backgroundColor = 0xFFF6EFE3)
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
