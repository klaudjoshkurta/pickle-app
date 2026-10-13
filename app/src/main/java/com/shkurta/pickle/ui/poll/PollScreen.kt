package com.shkurta.pickle.ui.poll

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.shkurta.pickle.R
import com.shkurta.pickle.data.PollRepository
import com.shkurta.pickle.data.sample.SamplePolls
import com.shkurta.pickle.domain.model.OptionResult
import com.shkurta.pickle.domain.model.OptionStatus
import com.shkurta.pickle.domain.model.Outcome
import com.shkurta.pickle.domain.model.Poll
import com.shkurta.pickle.domain.model.PollOption
import com.shkurta.pickle.domain.model.toResults
import com.shkurta.pickle.ui.components.ClosedColor
import com.shkurta.pickle.ui.components.SamplePollProvider
import com.shkurta.pickle.ui.components.StatusChip
import com.shkurta.pickle.ui.components.timeLabel
import com.shkurta.pickle.ui.components.voteSummary
import com.shkurta.pickle.ui.theme.PickleTheme

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PollScreen(
    pollId: String,
    onBack: () -> Unit,
    repository: PollRepository = PollRepository.Default,
) {
    val polls by repository.polls.collectAsState()
    val myVotes by repository.myVotes.collectAsState()
    val poll = polls.firstOrNull { it.id == pollId }
    val context = LocalContext.current
    val currentUserId = SamplePolls.CREATOR_ID

    if (poll == null) {
        PollNotFound(onBack = onBack)
        return
    }

    PollContent(
        poll = poll,
        currentUserId = currentUserId,
        myVoteOptionId = myVotes[pollId],
        onBack = onBack,
        onShare = { sharePoll(context, poll) },
        onVote = { optionId -> repository.vote(pollId, optionId) },
        onSuggest = { text -> repository.suggestOption(pollId, text, currentUserId) },
        onModerate = { optionId, approve -> repository.moderate(pollId, optionId, approve) },
        onClose = { repository.close(pollId) },
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun PollContent(
    poll: Poll,
    currentUserId: String,
    myVoteOptionId: String?,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onVote: (String) -> Unit,
    onSuggest: (String) -> OptionStatus?,
    onModerate: (optionId: String, approve: Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val isOpen = poll.isOpen()
    val isCreator = poll.isCreator(currentUserId)
    val hasVoted = myVoteOptionId != null
    val showResults = hasVoted || poll.settings.showResultsBeforeVoting || !isOpen
    val canVote = isOpen && (!hasVoted || poll.settings.allowVoteChange)

    var selectedOptionId by rememberSaveable(myVoteOptionId) { mutableStateOf(myVoteOptionId) }
    var showCloseDialog by rememberSaveable { mutableStateOf(false) }

    val results = remember(poll) { poll.toResults() }
    val rows = if (showResults) {
        results.options
    } else {
        poll.approvedOptions.map { OptionResult(option = it, percentage = 0f, isLeading = false) }
    }
    val pendingSuggestions = if (isCreator && isOpen) poll.pendingSuggestions else emptyList()

    Scaffold(
        topBar = {
            PollTopBar(
                canClose = isCreator && isOpen,
                onBack = onBack,
                onShare = onShare,
                onCloseClick = { showCloseDialog = true },
            )
        },
        bottomBar = {
            if (canVote) {
                Button(
                    onClick = { selectedOptionId?.let(onVote) },
                    enabled = selectedOptionId != null && selectedOptionId != myVoteOptionId,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp),
                ) {
                    Text(
                        text = if (hasVoted) "Change vote" else "Vote",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                PollHeader(poll = poll, isOpen = isOpen)
            }

            if (!isOpen) {
                item(key = "outcome") {
                    OutcomeBanner(outcome = results.outcome)
                }
            }

            items(
                items = rows,
                key = { "option_${it.option.id}" },
            ) { row ->
                OptionRow(
                    result = row,
                    showResults = showResults,
                    isOpen = isOpen,
                    selected = row.option.id == selectedOptionId,
                    isMyVote = row.option.id == myVoteOptionId,
                    enabled = canVote,
                    onClick = { selectedOptionId = row.option.id },
                )
            }

            if (isOpen && poll.settings.allowSuggestions) {
                item(key = "suggest") {
                    SuggestOption(onSuggest = onSuggest)
                }
            }

            if (pendingSuggestions.isNotEmpty()) {
                item(key = "pending_title") {
                    Text(
                        text = "Pending suggestions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                items(
                    items = pendingSuggestions,
                    key = { "pending_${it.id}" },
                ) { option ->
                    PendingSuggestionRow(
                        option = option,
                        onApprove = { onModerate(option.id, true) },
                        onReject = { onModerate(option.id, false) },
                    )
                }
            }
        }
    }

    if (showCloseDialog) {
        AlertDialog(
            onDismissRequest = { showCloseDialog = false },
            title = { Text("Close this poll?") },
            text = { Text("Voting ends now and the result becomes final.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCloseDialog = false
                        onClose()
                    },
                ) {
                    Text("Close poll")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun PollTopBar(
    canClose: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = {},
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                )
            }
        },
        actions = {
            if (canClose) {
                TextButton(onClick = onCloseClick) {
                    Text(
                        text = "Close poll",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            IconButton(onClick = onShare) {
                Icon(
                    painter = painterResource(R.drawable.ic_share),
                    contentDescription = "Share",
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun PollHeader(
    poll: Poll,
    isOpen: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
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
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = voteSummary(poll),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OutcomeBanner(
    outcome: Outcome,
    modifier: Modifier = Modifier,
) {
    val text = when (outcome) {
        Outcome.NoVotes -> "Closed with no votes"
        is Outcome.Winner -> "Winner: ${outcome.option.text}"
        is Outcome.Tie -> "It's a tie: ${outcome.options.joinToString(" · ") { it.text }}"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ClosedColor.copy(alpha = 0.10f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = ClosedColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        )
    }
}

@Composable
private fun OptionRow(
    result: OptionResult,
    showResults: Boolean,
    isOpen: Boolean,
    selected: Boolean,
    isMyVote: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = if (isOpen) MaterialTheme.colorScheme.primary else ClosedColor
    val fillColor = if (result.isLeading) {
        accentColor.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val animatedFill by animateFloatAsState(
        targetValue = if (showResults) result.percentage else 0f,
        animationSpec = tween(600),
        label = "fill",
    )

    Surface(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = if (selected) BorderStroke(2.dp, accentColor) else null,
    ) {
        Box {
            Box(
                modifier = Modifier
                    .matchParentSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedFill)
                        .background(fillColor),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = result.option.text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (result.isLeading) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                if (isMyVote) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = "Your vote",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp),
                    )
                }

                if (showResults) {
                    Text(
                        text = "${(result.percentage * 100).toInt()}% · ${result.option.voteCount}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (result.isLeading) {
                            accentColor
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestOption(
    onSuggest: (String) -> OptionStatus?,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var sentForApproval by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    sentForApproval = false
                },
                placeholder = { Text("Suggest an option") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f),
            )
            FilledIconButton(
                onClick = {
                    val status = onSuggest(text)
                    if (status != null) {
                        text = ""
                        sentForApproval = status == OptionStatus.PENDING
                    }
                },
                enabled = text.isNotBlank(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = "Add suggestion",
                )
            }
        }

        if (sentForApproval) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Sent to the poll creator for approval",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PendingSuggestionRow(
    option: PollOption,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = option.text,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onReject) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Reject",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onApprove) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = "Approve",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun PollNotFound(
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            PollTopBar(
                canClose = false,
                onBack = onBack,
                onShare = {},
                onCloseClick = {},
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Poll not found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun sharePoll(context: Context, poll: Poll) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "${poll.question}\nhttps://pickle.app/p/${poll.id}")
    }
    context.startActivity(Intent.createChooser(intent, null))
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, backgroundColor = 0xFFFAF2E3)
@Composable
private fun PollContentPreview(
    @PreviewParameter(SamplePollProvider::class) poll: Poll,
) {
    PickleTheme {
        PollContent(
            poll = poll,
            currentUserId = SamplePolls.CREATOR_ID,
            myVoteOptionId = null,
            onBack = {},
            onShare = {},
            onVote = {},
            onSuggest = { null },
            onModerate = { _, _ -> },
            onClose = {},
        )
    }
}
