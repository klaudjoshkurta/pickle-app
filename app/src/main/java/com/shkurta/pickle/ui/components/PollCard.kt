package com.shkurta.pickle.ui.components

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
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

@Composable
fun PollCard(
    poll: Poll,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    showSuggestionBadge: Boolean = true,
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
            StatusPill(isOpen = isOpen, openLabel = stringResource(R.string.live), closedLabel = stringResource(R.string.settled))
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
                text = stringResource(R.string.share_link),
                onClick = onShareClick,
                style = PickleButtonStyle.Teal,
                leadingIcon = R.drawable.ic_share,
                height = 40.dp,
            )
        } else {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.nobody_voted),
                style = MaterialTheme.typography.labelMedium,
                color = c.inkMuted,
            )
        }

        if (showSuggestionBadge && isOpen && pending > 0) {
            Spacer(Modifier.height(10.dp))
            InfoPill(
                text = pluralStringResource(R.plurals.suggestions_review, pending, pending),
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
@Composable
internal fun timeLabel(poll: Poll, now: Instant = Instant.now()): String? {
    val closedAt = poll.closedAt ?: return null
    val time = friendlyTime(closedAt, now)
    return stringResource(if(poll.isOpen(now)) R.string.closes_at else R.string.settled_at, time)
}

@Composable
internal fun friendlyTime(instant: Instant, now: Instant = Instant.now()): String {
    val zone = ZoneId.systemDefault()
    val date = instant.atZone(zone)
    val today = now.atZone(zone).toLocalDate()
    val time = android.text.format.DateFormat.getTimeFormat(androidx.compose.ui.platform.LocalContext.current).format(java.util.Date.from(instant))
    return when (date.toLocalDate()) {
        today -> stringResource(R.string.today_time, time)
        today.plusDays(1) -> stringResource(R.string.tomorrow_time, time)
        today.minusDays(1) -> stringResource(R.string.yesterday_time, time)
        else -> "${date.format(DateTimeFormatter.ofPattern("EEE d MMM", androidx.compose.ui.platform.LocalConfiguration.current.locales[0]))} $time"
    }
}

/** "11 votes · Detroit-style ahead by 2" */
@Composable
internal fun voteSummary(poll: Poll): String {
    val votes = poll.totalVotes
    val count = when {
        poll.expectedVoters != null -> stringResource(R.string.turnout, votes, poll.expectedVoters)
        poll.settings.votingMode == com.shkurta.pickle.domain.model.VotingMode.MULTIPLE -> voterSelectionLabel(votes, poll.totalSelections)
        votes == 0 -> stringResource(R.string.no_votes)
        else -> pluralStringResource(R.plurals.vote_count, votes, votes)
    }
    val results = poll.toResults()
    val lead = when (val outcome = results.outcome) {
        is Outcome.Winner -> {
            val margin = outcome.option.voteCount - (results.options.getOrNull(1)?.option?.voteCount ?: 0)
            stringResource(if(poll.status == PollStatus.CLOSED) R.string.option_won else R.string.option_ahead, outcome.option.text, margin)
        }
        is Outcome.Tie -> stringResource(R.string.tied_count, outcome.options.first().voteCount)
        Outcome.NoVotes -> null
    }
    return listOfNotNull(count, lead).joinToString(" · ")
}

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
    override val values = SamplePolls.all.asSequence()
}
