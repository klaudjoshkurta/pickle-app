package com.shkurta.pickle.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shkurta.pickle.data.sample.SamplePolls
import com.shkurta.pickle.domain.model.Poll
import com.shkurta.pickle.ui.theme.PickleTheme

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PollCard(
    poll: Poll = SamplePolls.pizzaNight,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusIndicator(
                isOpen = poll.isOpen()
            )

            Text(
                text = poll.question,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )

            val turnout = poll.turnout
            if (turnout != null) {
                LinearProgressIndicator(
                    progress = { turnout },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            Text(
                text = voteSummary(poll),
                style = MaterialTheme.typography.labelLarge, // 14sp, medium weight
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun StatusIndicator(
    modifier: Modifier = Modifier,
    isOpen: Boolean,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        shape = RoundedCornerShape(200.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Voting open",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun voteSummary(poll: Poll): String {
    val votes = poll.totalVotes
    return when {
        poll.expectedVoters != null -> "$votes of ${poll.expectedVoters} voted"
        votes == 0 -> "No votes yet"
        votes == 1 -> "1 vote"
        else -> "$votes votes"
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
private fun PollCardPreview(
    @PreviewParameter(SamplePollProvider::class) poll: Poll,
) {
    MaterialTheme {
        PollCard(
            poll = poll,
            modifier = Modifier.padding(16.dp)
        )
    }
}

class SamplePollProvider : PreviewParameterProvider<Poll> {
    @RequiresApi(Build.VERSION_CODES.O)
    override val values = SamplePolls.all.asSequence()
}