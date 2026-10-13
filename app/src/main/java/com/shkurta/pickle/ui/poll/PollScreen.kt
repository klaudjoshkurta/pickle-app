package com.shkurta.pickle.ui.poll

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.shkurta.pickle.R
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

private val CardShape = RoundedCornerShape(20.dp)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PollScreen(
    onBack: () -> Unit,
    viewModel: PollViewModel = hiltViewModel(),
) {
    val poll by viewModel.poll.collectAsState()
    val myVotes by viewModel.myVotes.collectAsState()
    val lastSuggestionStatus by viewModel.lastSuggestionStatus.collectAsState()
    val context = LocalContext.current

    if (poll == null) {
        PollNotFound(onBack = onBack)
        return
    }

    PollContent(
        poll = poll!!,
        currentUserId = viewModel.currentUserId,
        myVoteOptionId = myVotes[poll!!.id],
        lastSuggestionStatus = lastSuggestionStatus,
        onBack = onBack,
        onShare = { sharePoll(context, poll!!) },
        onVote = { optionId -> viewModel.vote(optionId) },
        onSuggest = { text ->
            viewModel.suggestOption(text)
        },
        onClearSuggestionStatus = { viewModel.clearSuggestionStatus() },
        onModerate = { optionId, approve -> viewModel.moderate(optionId, approve) },
        onClose = { viewModel.close() },
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun PollContent(
    poll: Poll,
    currentUserId: String,
    myVoteOptionId: String?,
    lastSuggestionStatus: OptionStatus?,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onVote: (String) -> Unit,
    onSuggest: (String) -> Unit,
    onClearSuggestionStatus: () -> Unit,
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
                        text = when {
                            selectedOptionId == null -> "Pick an option"
                            hasVoted -> "Change vote"
                            else -> "Vote"
                        },
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "header") {
                PollHeader(poll = poll, isOpen = isOpen)
            }

            if (!isOpen) {
                item(key = "outcome") {
                    OutcomeCard(outcome = results.outcome)
                }
            }

            item(key = "options_label") {
                SectionLabel(text = if (showResults) "Results" else "Options")
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
                    SuggestOption(
                        lastSuggestionStatus = lastSuggestionStatus,
                        onSuggest = onSuggest,
                        onClearStatus = onClearSuggestionStatus,
                    )
                }
            }

            if (pendingSuggestions.isNotEmpty()) {
                item(key = "pending_label") {
                    SectionLabel(text = "Pending suggestions (${pendingSuggestions.size})")
                }
                item(key = "pending") {
                    PendingSuggestionsCard(
                        suggestions = pendingSuggestions,
                        onModerate = onModerate,
                    )
                }
            }
        }
    }

    if (showCloseDialog) {
        AlertDialog(
            onDismissRequest = { showCloseDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
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

            Spacer(Modifier.height(16.dp))

            Text(
                text = poll.question,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(18.dp))

            val turnout = poll.turnout
            if (turnout != null) {
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

                Spacer(Modifier.height(10.dp))
            }

            Text(
                text = voteSummary(poll),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OutcomeCard(
    outcome: Outcome,
    modifier: Modifier = Modifier,
) {
    val isWinner = outcome is Outcome.Winner
    val label = when (outcome) {
        Outcome.NoVotes -> "Closed"
        is Outcome.Winner -> "Winner"
        is Outcome.Tie -> "It's a tie"
    }
    val text = when (outcome) {
        Outcome.NoVotes -> "Nobody voted"
        is Outcome.Winner -> outcome.option.text
        is Outcome.Tie -> outcome.options.joinToString(" · ") { it.text }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isWinner) MaterialTheme.colorScheme.primary else ClosedColor.copy(alpha = 0.12f),
        contentColor = if (isWinner) MaterialTheme.colorScheme.onPrimary else ClosedColor,
        shape = CardShape,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
        modifier = modifier.padding(start = 4.dp, top = 14.dp),
    )
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
        accentColor.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f)
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
        border = if (selected && enabled) BorderStroke(2.dp, accentColor) else null,
    ) {
        Box {
            Box(modifier = Modifier.matchParentSize()) {
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
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (enabled) {
                    SelectionDot(selected = selected, color = accentColor)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.option.text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (result.isLeading) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isMyVote) {
                        Text(
                            text = "Your vote",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor,
                        )
                    }
                }

                if (showResults) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${(result.percentage * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (result.isLeading) {
                                accentColor
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                        Text(
                            text = voteCountLabel(result.option.voteCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionDot(
    selected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(22.dp)
            .then(
                if (selected) {
                    Modifier.background(color, CircleShape)
                } else {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f),
                        shape = CircleShape,
                    )
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun SuggestOption(
    lastSuggestionStatus: OptionStatus?,
    onSuggest: (String) -> Unit,
    onClearStatus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val sentForApproval = lastSuggestionStatus == OptionStatus.PENDING
    val submit = {
        if (text.isNotBlank()) {
            onSuggest(text)
            text = ""
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                modifier = Modifier.padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = text,
                    onValueChange = {
                        text = it
                        onClearStatus()
                    },
                    placeholder = { Text("Suggest an option") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = submit,
                    enabled = text.isNotBlank(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = "Add suggestion",
                        tint = if (text.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f)
                        },
                    )
                }
            }
        }

        if (sentForApproval) {
            Text(
                text = "Sent to the poll creator for approval",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
        }
    }
}

@Composable
private fun PendingSuggestionsCard(
    suggestions: List<PollOption>,
    onModerate: (optionId: String, approve: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
    ) {
        Column {
            suggestions.forEachIndexed { index, option ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = option.text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    ModerationButton(
                        iconRes = R.drawable.ic_close,
                        contentDescription = "Reject ${option.text}",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = { onModerate(option.id, false) },
                    )
                    ModerationButton(
                        iconRes = R.drawable.ic_check,
                        contentDescription = "Approve ${option.text}",
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        onClick = { onModerate(option.id, true) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ModerationButton(
    iconRes: Int,
    contentDescription: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(36.dp),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(18.dp),
            )
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

private fun voteCountLabel(count: Int): String = if (count == 1) "1 vote" else "$count votes"

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
            lastSuggestionStatus = null,
            onBack = {},
            onShare = {},
            onVote = {},
            onSuggest = {},
            onClearSuggestionStatus = {},
            onModerate = { _, _ -> },
            onClose = {},
        )
    }
}
