package com.shkurta.pickle.ui.poll

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
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
import com.shkurta.pickle.domain.model.PollResults
import com.shkurta.pickle.domain.model.toResults
import com.shkurta.pickle.ui.components.BlockMeter
import com.shkurta.pickle.ui.components.CircleIconButton
import com.shkurta.pickle.ui.components.DashedActionButton
import com.shkurta.pickle.ui.components.InfoPill
import com.shkurta.pickle.ui.components.PickleBorderWidth
import com.shkurta.pickle.ui.components.PickleButton
import com.shkurta.pickle.ui.components.PickleButtonStyle
import com.shkurta.pickle.ui.components.PickleCard
import com.shkurta.pickle.ui.components.PickleCardShape
import com.shkurta.pickle.ui.components.PickleHeadline
import com.shkurta.pickle.ui.components.PickleTextField
import com.shkurta.pickle.ui.components.SamplePollProvider
import com.shkurta.pickle.ui.components.StatusPill
import com.shkurta.pickle.ui.components.dashedBorder
import com.shkurta.pickle.ui.components.pollLink
import com.shkurta.pickle.ui.components.sharePoll
import com.shkurta.pickle.ui.components.timeLabel
import com.shkurta.pickle.ui.components.voteSummary
import com.shkurta.pickle.ui.theme.Pickle
import com.shkurta.pickle.ui.theme.PickleTheme

/** Above this many votes the leader meter switches from one block per vote to a 12-block scale. */
private const val MAX_METER_BLOCKS = 12

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

@OptIn(ExperimentalMaterial3Api::class)
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
    val c = Pickle.colors
    val isOpen = poll.isOpen()
    val isCreator = poll.isCreator(currentUserId)
    val hasVoted = myVoteOptionId != null
    val showResults = hasVoted || poll.settings.showResultsBeforeVoting || !isOpen
    val canVote = isOpen && (!hasVoted || poll.settings.allowVoteChange)

    var isChangingVote by rememberSaveable(myVoteOptionId) { mutableStateOf(false) }
    val isVoting = canVote && (!hasVoted || isChangingVote)

    var selectedOptionId by rememberSaveable(myVoteOptionId) { mutableStateOf(myVoteOptionId) }
    var showCloseSheet by rememberSaveable { mutableStateOf(false) }

    val results = remember(poll) { poll.toResults() }
    // While picking, only show the live tally underneath once there's something to show
    val showResultsSection = showResults && (!isVoting || results.totalVotes > 0)
    val pendingSuggestions = if (isCreator && isOpen) poll.pendingSuggestions else emptyList()
    val selectedOption = poll.approvedOptions.firstOrNull { it.id == selectedOptionId }

    Scaffold(
        containerColor = c.canvas,
        topBar = {
            PollTopBar(
                onBack = onBack,
                onShare = onShare,
            )
        },
        bottomBar = {
            if (isVoting) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    PickleButton(
                        text = when {
                            selectedOption == null -> "Pick an option"
                            hasVoted -> "Change to ${selectedOption.text}"
                            else -> "Vote for ${selectedOption.text}"
                        },
                        onClick = {
                            selectedOptionId?.let(onVote)
                            isChangingVote = false
                        },
                        enabled = selectedOptionId != null && selectedOptionId != myVoteOptionId,
                        style = PickleButtonStyle.Teal,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (isChangingVote) {
                        PickleButton(
                            text = "Keep my vote",
                            onClick = {
                                selectedOptionId = myVoteOptionId
                                isChangingVote = false
                            },
                            style = PickleButtonStyle.Ghost,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
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
            if (hasVoted && isOpen && !isVoting) {
                item(key = "youre_in") {
                    YoureInBanner(
                        optionText = poll.approvedOptions.firstOrNull { it.id == myVoteOptionId }?.text,
                        canChange = poll.settings.allowVoteChange,
                        onChange = { isChangingVote = true },
                    )
                }
            }

            item(key = "header") {
                PollHeader(poll = poll, isOpen = isOpen)
            }

            if (isVoting) {
                item(key = "pick_label") {
                    PickOneLabel(resultsHidden = !showResults)
                }
                items(
                    items = poll.approvedOptions,
                    key = { "vote_${it.id}" },
                ) { option ->
                    VoteOptionRow(
                        option = option,
                        selected = option.id == selectedOptionId,
                        onClick = { selectedOptionId = option.id },
                    )
                }
                if (poll.settings.allowSuggestions) {
                    item(key = "suggest") {
                        SuggestOption(
                            lastSuggestionStatus = lastSuggestionStatus,
                            onSuggest = onSuggest,
                            onClearStatus = onClearSuggestionStatus,
                        )
                    }
                }
            }

            if (showResultsSection) {
                if (results.totalVotes == 0) {
                    item(key = "no_votes") {
                        NoVotesCard(isOpen = isOpen)
                    }
                } else {
                    item(key = "leader") {
                        LeaderCard(
                            results = results,
                            isOpen = isOpen,
                            myVoteOptionId = myVoteOptionId,
                        )
                    }
                    val rest = results.options.drop(leaderCount(results))
                    if (rest.isNotEmpty()) {
                        item(key = "rest") {
                            ResultsList(
                                rows = rest,
                                myVoteOptionId = myVoteOptionId,
                            )
                        }
                    }
                    item(key = "tally") {
                        TallyLine(results = results, isOpen = isOpen)
                    }
                }

                if (!isVoting && isOpen && poll.settings.allowSuggestions && !isCreator) {
                    item(key = "suggest_after") {
                        SuggestOption(
                            lastSuggestionStatus = lastSuggestionStatus,
                            onSuggest = onSuggest,
                            onClearStatus = onClearSuggestionStatus,
                        )
                    }
                }
            }

            if (pendingSuggestions.isNotEmpty()) {
                item(key = "pending") {
                    PendingSuggestionsCard(
                        suggestions = pendingSuggestions,
                        onModerate = onModerate,
                    )
                }
            }

            if (isCreator && isOpen) {
                item(key = "link") {
                    LinkCard(pollId = poll.id, onShare = onShare)
                }
                item(key = "end") {
                    Column {
                        PickleButton(
                            text = "End voting",
                            onClick = { showCloseSheet = true },
                            style = PickleButtonStyle.Outline,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Ending locks the count and shows everyone the result.",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.inkMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    if (showCloseSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCloseSheet = false },
            containerColor = c.surface,
            contentColor = c.ink,
        ) {
            EndVotingSheet(
                results = results,
                onEnd = {
                    showCloseSheet = false
                    onClose()
                },
                onKeepOpen = { showCloseSheet = false },
            )
        }
    }
}

private fun leaderCount(results: PollResults): Int = when (val outcome = results.outcome) {
    is Outcome.Winner -> 1
    is Outcome.Tie -> outcome.options.size
    Outcome.NoVotes -> 0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PollTopBar(
    onBack: () -> Unit,
    onShare: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier.padding(horizontal = 8.dp),
        title = {},
        navigationIcon = {
            CircleIconButton(
                iconRes = R.drawable.ic_arrow_back,
                contentDescription = "Back",
                onClick = onBack,
            )
        },
        actions = {
            if (onShare != null) {
                CircleIconButton(
                    iconRes = R.drawable.ic_share,
                    contentDescription = "Share",
                    onClick = onShare,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Pickle.colors.canvas,
            titleContentColor = Pickle.colors.ink,
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
    val c = Pickle.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusPill(isOpen = isOpen)
            val label = timeLabel(poll)
            if (label != null) {
                InfoPill(text = label, icon = R.drawable.ic_clock)
            }
        }
        Spacer(Modifier.height(12.dp))
        PickleHeadline(text = poll.question)
        Spacer(Modifier.height(8.dp))
        Text(
            text = voteSummary(poll),
            style = MaterialTheme.typography.labelMedium,
            color = c.inkMuted,
        )
    }
}

@Composable
private fun YoureInBanner(
    optionText: String?,
    canChange: Boolean,
    onChange: () -> Unit,
) {
    val c = Pickle.colors
    PickleCard(color = c.mustard) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "You're in!",
                    style = MaterialTheme.typography.titleSmall,
                    color = c.onMustard,
                )
                if (optionText != null) {
                    Text(
                        text = if (canChange) "You picked $optionText · you can change it until it closes"
                        else "You picked $optionText",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.onMustard,
                    )
                }
            }
            if (canChange) {
                Spacer(Modifier.width(12.dp))
                PickleButton(
                    text = "Change",
                    onClick = onChange,
                    style = PickleButtonStyle.Outline,
                    height = 36.dp,
                )
            }
        }
    }
}

@Composable
private fun PickOneLabel(resultsHidden: Boolean) {
    val c = Pickle.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Pick one",
            style = MaterialTheme.typography.labelLarge,
            color = c.ink,
            modifier = Modifier.weight(1f),
        )
        if (resultsHidden) {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = null,
                tint = c.inkMuted,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Results show after you vote",
                style = MaterialTheme.typography.labelSmall,
                color = c.inkMuted,
            )
        }
    }
}

@Composable
private fun VoteOptionRow(
    option: PollOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val c = Pickle.colors
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = if (selected) c.tealSoft else c.surface,
        contentColor = c.ink,
        shape = PickleCardShape,
        border = BorderStroke(if (selected) 2.dp else PickleBorderWidth, if (selected) c.teal else c.lineStrong),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioDot(selected = selected)
            Text(
                text = option.text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    val c = Pickle.colors
    Box(
        modifier = Modifier
            .size(22.dp)
            .then(
                if (selected) Modifier.background(c.teal, CircleShape)
                else Modifier.border(PickleBorderWidth, c.lineStrong, CircleShape)
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = c.onBrand,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/** Big tomato card for the leader (or winner), with the mustard ribbon. */
@Composable
private fun LeaderCard(
    results: PollResults,
    isOpen: Boolean,
    myVoteOptionId: String?,
) {
    val c = Pickle.colors
    val leaders = results.options.take(leaderCount(results))
    val isTie = leaders.size > 1
    val top = leaders.first()
    val runnerUp = results.options.getOrNull(leaders.size)?.option?.voteCount ?: 0
    val margin = top.option.voteCount - runnerUp
    val ribbon = when {
        isTie -> "TIED"
        isOpen -> "IN THE LEAD"
        else -> "WINNER"
    }

    Column {
        if (!isOpen) {
            Text(
                text = if (isTie) "Dead heat!" else "The crew picked",
                style = MaterialTheme.typography.labelLarge,
                color = c.inkMuted,
                modifier = Modifier.padding(start = 2.dp, top = 6.dp, bottom = 6.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PickleCardShape)
                .background(c.tomato)
                .border(PickleBorderWidth, c.lineStrong, PickleCardShape),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = leaders.joinToString("  ·  ") { it.option.text },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = c.onBrand,
                    modifier = Modifier.padding(end = 72.dp),
                )
                if (leaders.any { it.option.id == myVoteOptionId }) {
                    Spacer(Modifier.height(8.dp))
                    InfoPill(
                        text = "Your pick",
                        container = c.tomatoDeep,
                        content = c.onBrand,
                        borderColor = null,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    val animated by animateFloatAsState(
                        targetValue = top.percentage,
                        animationSpec = tween(600),
                        label = "leaderPct",
                    )
                    Text(
                        text = "${(animated * 100).toInt()}%",
                        style = MaterialTheme.typography.displayMedium,
                        color = c.onBrand,
                        modifier = Modifier.weight(1f),
                    )
                    InfoPill(
                        text = buildString {
                            append("${top.option.voteCount} of ${results.totalVotes} votes")
                            if (!isTie && margin > 0) append(if (isOpen) " · ahead by $margin" else " · won by $margin")
                        },
                        container = c.tomatoDeep,
                        content = c.onBrand,
                        borderColor = null,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                val blocks = minOf(results.totalVotes, MAX_METER_BLOCKS)
                val filled = if (results.totalVotes <= MAX_METER_BLOCKS) top.option.voteCount
                else Math.round(top.percentage * MAX_METER_BLOCKS)
                BlockMeter(
                    filled = filled,
                    total = blocks,
                    color = c.mustard,
                    trackColor = c.tomatoDeep,
                )
            }
            Ribbon(
                text = ribbon,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
private fun Ribbon(text: String, modifier: Modifier = Modifier) {
    val c = Pickle.colors
    Box(
        modifier = modifier
            .offset(x = 38.dp, y = 22.dp)
            .rotate(40f)
            .width(160.dp)
            .background(c.mustard)
            .border(PickleBorderWidth, c.lineStrong)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = c.onMustard,
        )
    }
}

@Composable
private fun ResultsList(
    rows: List<OptionResult>,
    myVoteOptionId: String?,
) {
    val c = Pickle.colors
    PickleCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
        rows.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(color = c.lineSoft)
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = row.option.text,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (row.option.id == myVoteOptionId) {
                            Text(
                                text = "Your pick",
                                style = MaterialTheme.typography.labelSmall,
                                color = c.teal,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${(row.percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = voteCountLabel(row.option.voteCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = c.inkMuted,
                    )
                }
                Spacer(Modifier.height(8.dp))
                val animated by animateFloatAsState(
                    targetValue = row.percentage,
                    animationSpec = tween(600),
                    label = "rowPct",
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(c.sunken),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animated)
                            .height(6.dp)
                            .background(c.teal, CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun TallyLine(results: PollResults, isOpen: Boolean) {
    val c = Pickle.colors
    Row(
        modifier = Modifier.padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = voteCountLabel(results.totalVotes) + " in",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = c.ink,
        )
        if (isOpen) {
            Text(
                text = " — still anyone's game",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = c.textAccent,
                modifier = Modifier.weight(1f),
            )
            InfoPill(
                text = "Live",
                container = c.canvas,
                content = c.teal,
                borderColor = null,
            )
        }
    }
}

@Composable
private fun NoVotesCard(isOpen: Boolean) {
    val c = Pickle.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .dashedBorder(c.lineStrong, 16.dp)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (isOpen) "No votes yet" else "Nobody voted",
            style = MaterialTheme.typography.titleSmall,
            color = c.ink,
        )
        if (isOpen) {
            Text(
                text = "Share the link and watch them land.",
                style = MaterialTheme.typography.bodySmall,
                color = c.inkMuted,
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
    val c = Pickle.colors
    var expanded by rememberSaveable { mutableStateOf(false) }
    var text by rememberSaveable { mutableStateOf("") }
    val sentForApproval = lastSuggestionStatus == OptionStatus.PENDING
    val submit = {
        if (text.isNotBlank()) {
            onSuggest(text.trim())
            text = ""
            expanded = false
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (expanded) {
            PickleCard {
                Text(
                    text = "Suggest an option",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(10.dp))
                PickleTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        onClearStatus()
                    },
                    placeholder = "Your idea",
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PickleButton(
                        text = "Cancel",
                        onClick = {
                            text = ""
                            expanded = false
                        },
                        style = PickleButtonStyle.Outline,
                        height = 44.dp,
                        modifier = Modifier.weight(1f),
                    )
                    PickleButton(
                        text = "Send",
                        onClick = submit,
                        enabled = text.isNotBlank(),
                        style = PickleButtonStyle.Teal,
                        height = 44.dp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            DashedActionButton(
                text = "Suggest an option",
                onClick = { expanded = true },
            )
        }

        if (sentForApproval) {
            Text(
                text = "Your idea is with the poll creator. If it's added, you can switch your vote to it.",
                style = MaterialTheme.typography.labelSmall,
                color = c.inkMuted,
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
    val c = Pickle.colors
    PickleCard(modifier = modifier) {
        suggestions.forEachIndexed { index, option ->
            if (index > 0) {
                HorizontalDivider(
                    color = c.lineSoft,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            Text(
                text = "Suggested: “${option.text}”",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "Add it and it joins with 0 votes — your call.",
                style = MaterialTheme.typography.labelSmall,
                color = c.inkMuted,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PickleButton(
                    text = "Add it",
                    onClick = { onModerate(option.id, true) },
                    style = PickleButtonStyle.Teal,
                    height = 40.dp,
                    modifier = Modifier.weight(1f),
                )
                PickleButton(
                    text = "Not this time",
                    onClick = { onModerate(option.id, false) },
                    style = PickleButtonStyle.Outline,
                    height = 40.dp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LinkCard(pollId: String, onShare: () -> Unit) {
    val c = Pickle.colors
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val link = pollLink(pollId)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PickleCardShape)
            .background(c.sunken)
            .padding(16.dp),
    ) {
        Text(
            text = "Anyone with the link can vote",
            style = MaterialTheme.typography.labelSmall,
            color = c.inkMuted,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = link.removePrefix("https://"),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            PickleButton(
                text = if (copied) "Copied" else "Copy link",
                onClick = {
                    clipboard.setText(AnnotatedString(link))
                    copied = true
                },
                height = 40.dp,
            )
        }
        Spacer(Modifier.height(8.dp))
        PickleButton(
            text = "Share…",
            onClick = onShare,
            style = PickleButtonStyle.Outline,
            leadingIcon = R.drawable.ic_share,
            height = 40.dp,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun EndVotingSheet(
    results: PollResults,
    onEnd: () -> Unit,
    onKeepOpen: () -> Unit,
) {
    val c = Pickle.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
    ) {
        PickleHeadline(
            text = "End voting now?",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Ending locks the count and shows the result to everyone.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.inkMuted,
        )
        val standing = when (val outcome = results.outcome) {
            is Outcome.Winner -> {
                val runnerUp = results.options.getOrNull(1)?.option?.voteCount ?: 0
                "${outcome.option.text} is ahead ${outcome.option.voteCount}–$runnerUp with ${voteCountLabel(results.totalVotes)}"
            }
            is Outcome.Tie -> "It's a tie at ${outcome.options.first().voteCount} votes each"
            Outcome.NoVotes -> "Nobody has voted yet"
        }
        Spacer(Modifier.height(16.dp))
        PickleCard(color = c.canvas, borderColor = c.lineSoft) {
            Text(
                text = standing,
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Spacer(Modifier.height(20.dp))
        PickleButton(
            text = "End & reveal",
            onClick = onEnd,
            style = PickleButtonStyle.Tomato,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        PickleButton(
            text = "Keep it open",
            onClick = onKeepOpen,
            style = PickleButtonStyle.Outline,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PollNotFound(
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = Pickle.colors.canvas,
        topBar = {
            PollTopBar(
                onBack = onBack,
                onShare = null,
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
                color = Pickle.colors.inkMuted,
            )
        }
    }
}

private fun voteCountLabel(count: Int): String = if (count == 1) "1 vote" else "$count votes"

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, backgroundColor = 0xFFF6EFE3)
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
