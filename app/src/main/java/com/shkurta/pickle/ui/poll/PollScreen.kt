package com.shkurta.pickle.ui.poll

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.shkurta.pickle.ui.components.ActionFrame
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
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
import com.shkurta.pickle.ui.components.resultCountLabel
import com.shkurta.pickle.ui.components.voterSelectionLabel
import com.shkurta.pickle.ui.components.percentageLabel
import com.shkurta.pickle.ui.components.timeLabel
import com.shkurta.pickle.ui.components.voteSummary
import com.shkurta.pickle.ui.theme.Pickle
import com.shkurta.pickle.ui.theme.PickleTheme

/** Above this many votes the leader meter switches from one block per vote to a 12-block scale. */
private const val MAX_METER_BLOCKS = 12

@Composable
fun PollScreen(
    onBack: () -> Unit,
    isDemo: Boolean = false,
    onSignIn: () -> Unit = {},
    onTemplateSaved: () -> Unit = {},
    viewModel: PollViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val poll = state.poll
    var showTemplateDialog by rememberSaveable { mutableStateOf(false) }
    val templateId by viewModel.savedTemplateId.collectAsStateWithLifecycle()
    val templates by viewModel.personalTemplates.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(templateId) {
        if(templateId != null) { showTemplateDialog=false; viewModel.clearSavedTemplate(); onTemplateSaved() }
    }
    if(showTemplateDialog && poll != null) SaveTemplateDialog(poll, templates.firstOrNull { it.id==poll.templateId || it.sourcePollId==poll.id },{showTemplateDialog=false},viewModel::saveTemplate)
    val context = LocalContext.current

    val action by viewModel.actionState.collectAsStateWithLifecycle()
    ActionFrame(action, viewModel::retry) {
        Column {
            if (isDemo) {
                Text(stringResource(R.string.offline_results))
                PickleButton(text = stringResource(R.string.sign_in_live), onClick = onSignIn)
            }
            if (poll == null || poll.options.size < com.shkurta.pickle.domain.model.MIN_POLL_OPTIONS) {
                PollNotFound(onBack = onBack, onRefresh = viewModel::refresh)
                return@ActionFrame
            }

            PollContent(
                state = if (isDemo) state.copy(isDemo = true, isCreator = false) else state,
                onBack = onBack,
                onShare = { if (!isDemo) sharePoll(context, poll) },
                onVote = viewModel::vote,
                onSelectOption = viewModel::selectOption,
                onChangeVote = viewModel::changeVote,
                onKeepVote = viewModel::keepVote,
                onSuggest = { text ->
                    viewModel.suggestOption(text)
                },
                onClearSuggestionStatus = { viewModel.clearSuggestionStatus() },
                onModerate = { optionId, approve -> viewModel.moderate(optionId, approve) },
                onClose = { viewModel.close() },
                onSaveTemplate = { showTemplateDialog=true },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PollContent(
    state: PollUiState,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onVote: () -> Unit,
    onSelectOption: (String) -> Unit,
    onChangeVote: () -> Unit,
    onKeepVote: () -> Unit,
    onSuggest: (String) -> Unit,
    onClearSuggestionStatus: () -> Unit,
    onModerate: (optionId: String, approve: Boolean) -> Unit,
    onClose: () -> Unit,
    onSaveTemplate: () -> Unit = {},
) {
    val c = Pickle.colors
    val poll = state.poll ?: return
    val results = state.results ?: return
    val myVoteOptionIds = state.myVoteOptionIds
    val lastSuggestionStatus = state.lastSuggestionStatus
    val isOpen = state.isOpen
    val isCreator = state.isCreator
    val hasVoted = state.hasVoted
    val showResults = state.showResults
    val isChangingVote = state.isChangingVote
    val isVoting = state.isVoting
    val selectedOptionIds = state.selectedOptionIds
    val multiple = poll.settings.votingMode == com.shkurta.pickle.domain.model.VotingMode.MULTIPLE
    var showCloseSheet by rememberSaveable { mutableStateOf(false) }

    val showResultsSection = state.showResultsSection
    val pendingSuggestions = state.pendingSuggestions
    val selectedOption = state.selectedOption

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
                            selectedOptionIds.isEmpty() -> stringResource(R.string.pick_option)
                            multiple -> pluralStringResource(if(hasVoted) R.plurals.update_selections else R.plurals.submit_selections, selectedOptionIds.size, selectedOptionIds.size)
                            hasVoted -> stringResource(R.string.change_to, selectedOption?.text.orEmpty())
                            else -> stringResource(R.string.vote_for, selectedOption?.text.orEmpty())
                        },
                        onClick = onVote,
                        enabled = selectedOptionIds.isNotEmpty() && selectedOptionIds != myVoteOptionIds,
                        style = PickleButtonStyle.Teal,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (isChangingVote) {
                        PickleButton(
                            text = stringResource(R.string.keep_vote),
                            onClick = onKeepVote,
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
                        optionText = poll.approvedOptions.filter { it.id in myVoteOptionIds }.joinToString { it.text },
                        canChange = poll.settings.allowVoteChange,
                        onChange = onChangeVote,
                    )
                }
            }

            item(key = "header") {
                PollHeader(poll = poll, isOpen = isOpen)
            }

            if (isVoting) {
                item(key = "pick_label") {
                    PickOneLabel(resultsHidden = !showResults, multiple = multiple)
                }
                items(
                    items = poll.approvedOptions,
                    key = { "vote_${it.id}" },
                ) { option ->
                    VoteOptionRow(
                        option = option,
                        selected = option.id in selectedOptionIds,
                        multiple = multiple,
                        onClick = { onSelectOption(option.id) },
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
                            myVoteOptionIds = myVoteOptionIds,
                        )
                    }
                    val rest = results.options.drop(leaderCount(results))
                    if (rest.isNotEmpty()) {
                        item(key = "rest") {
                            ResultsList(
                                rows = rest,
                                myVoteOptionIds = myVoteOptionIds,
                            )
                        }
                    }
                    item(key = "tally") {
                        TallyLine(results = results, isOpen = isOpen)
                    }
                }

                if (!state.isDemo && !isVoting && isOpen && poll.settings.allowSuggestions && !isCreator) {
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

            if (isCreator) {
                item(key="save_template") { PickleButton(stringResource(R.string.save_as_template),onSaveTemplate,style=PickleButtonStyle.Outline,modifier=Modifier.fillMaxWidth()) }
            }
            if (isCreator && isOpen) {
                item(key = "link") {
                    LinkCard(pollId = poll.id, onShare = onShare)
                }
                item(key = "end") {
                    Column {
                        PickleButton(
                            text = stringResource(R.string.end_voting),
                            onClick = { showCloseSheet = true },
                            style = PickleButtonStyle.Outline,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.ending_description),
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
                contentDescription = stringResource(R.string.back),
                onClick = onBack,
            )
        },
        actions = {
            if (onShare != null) {
                CircleIconButton(
                    iconRes = R.drawable.ic_share,
                    contentDescription = stringResource(R.string.share),
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
                    text = stringResource(R.string.youre_in),
                    style = MaterialTheme.typography.titleSmall,
                    color = c.onMustard,
                )
                if (optionText != null) {
                    Text(
                        text = if (canChange) stringResource(R.string.picked_changeable, optionText)
                        else stringResource(R.string.picked, optionText),
                        style = MaterialTheme.typography.labelSmall,
                        color = c.onMustard,
                    )
                }
            }
            if (canChange) {
                Spacer(Modifier.width(12.dp))
                PickleButton(
                    text = stringResource(R.string.change),
                    onClick = onChange,
                    style = PickleButtonStyle.Outline,
                    height = 36.dp,
                )
            }
        }
    }
}

@Composable
private fun PickOneLabel(resultsHidden: Boolean, multiple: Boolean = false) {
    val c = Pickle.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if(multiple) stringResource(R.string.pick_all) else stringResource(R.string.pick_one),
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
                text = stringResource(R.string.results_after_vote),
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
    multiple: Boolean = false,
    onClick: () -> Unit,
) {
    val c = Pickle.colors
    Surface(
        modifier = Modifier.fillMaxWidth().then(if(multiple) Modifier.toggleable(value=selected,role=Role.Checkbox,onValueChange={onClick()}) else Modifier.selectable(selected=selected,role=Role.RadioButton,onClick=onClick)),
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
            if(multiple) androidx.compose.material3.Checkbox(checked=selected,onCheckedChange=null) else RadioDot(selected = selected)
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
    myVoteOptionIds: Set<String>,
) {
    val c = Pickle.colors
    val leaders = results.options.take(leaderCount(results))
    val isTie = leaders.size > 1
    val top = leaders.first()
    val runnerUp = results.options.getOrNull(leaders.size)?.option?.voteCount ?: 0
    val margin = top.option.voteCount - runnerUp
    val ribbon = when {
        isTie -> stringResource(R.string.tied_badge)
        isOpen -> stringResource(R.string.lead_badge)
        else -> stringResource(R.string.winner_badge)
    }

    Column {
        if (!isOpen) {
            Text(
                text = if (isTie) stringResource(R.string.dead_heat) else stringResource(R.string.crew_picked),
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
                if (leaders.any { it.option.id in myVoteOptionIds }) {
                    Spacer(Modifier.height(8.dp))
                    InfoPill(
                        text = stringResource(R.string.your_pick),
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
                        text = percentageLabel((animated * 100).toInt()),
                        style = MaterialTheme.typography.displayMedium,
                        color = c.onBrand,
                        modifier = Modifier.weight(1f),
                    )
                    InfoPill(
                        text = buildString {
                            append(resultCountLabel(top.option.voteCount, results.totalVotes, results.votingMode==com.shkurta.pickle.domain.model.VotingMode.MULTIPLE))
                            if (!isTie && margin > 0) append(if (isOpen) stringResource(R.string.ahead_margin, margin) else stringResource(R.string.won_margin, margin))
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
    myVoteOptionIds: Set<String>,
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
                        if (row.option.id in myVoteOptionIds) {
                            Text(
                                text = stringResource(R.string.your_pick),
                                style = MaterialTheme.typography.labelSmall,
                                color = c.teal,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = percentageLabel((row.percentage * 100).toInt()),
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
            text = if(results.votingMode==com.shkurta.pickle.domain.model.VotingMode.MULTIPLE) voterSelectionLabel(results.totalVotes, results.totalSelections) else stringResource(R.string.votes_in, voteCountLabel(results.totalVotes)),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = c.ink,
        )
        if (isOpen) {
            Text(
                text = stringResource(R.string.anyones_game),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = c.textAccent,
                modifier = Modifier.weight(1f),
            )
            InfoPill(
                text = stringResource(R.string.live),
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
            text = if (isOpen) stringResource(R.string.no_votes) else stringResource(R.string.nobody_voted),
            style = MaterialTheme.typography.titleSmall,
            color = c.ink,
        )
        if (isOpen) {
            Text(
                text = stringResource(R.string.share_watch),
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
                    text = stringResource(R.string.suggest_option),
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(10.dp))
                PickleTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        onClearStatus()
                    },
                    placeholder = stringResource(R.string.your_idea),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PickleButton(
                        text = stringResource(R.string.cancel),
                        onClick = {
                            text = ""
                            expanded = false
                        },
                        style = PickleButtonStyle.Outline,
                        height = 44.dp,
                        modifier = Modifier.weight(1f),
                    )
                    PickleButton(
                        text = stringResource(R.string.send),
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
                text = stringResource(R.string.suggest_option),
                onClick = { expanded = true },
            )
        }

        if (sentForApproval) {
            Text(
                text = stringResource(R.string.suggestion_sent),
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
                text = stringResource(R.string.suggested_option, option.text),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.suggestion_review),
                style = MaterialTheme.typography.labelSmall,
                color = c.inkMuted,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PickleButton(
                    text = stringResource(R.string.add_it),
                    onClick = { onModerate(option.id, true) },
                    style = PickleButtonStyle.Teal,
                    height = 40.dp,
                    modifier = Modifier.weight(1f),
                )
                PickleButton(
                    text = stringResource(R.string.not_this_time),
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
            text = stringResource(R.string.anyone_link),
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
                text = if (copied) stringResource(R.string.copied) else stringResource(R.string.copy_link),
                onClick = {
                    clipboard.setText(AnnotatedString(link))
                    copied = true
                },
                height = 40.dp,
            )
        }
        Spacer(Modifier.height(8.dp))
        PickleButton(
            text = stringResource(R.string.share_ellipsis),
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
            text = stringResource(R.string.end_now),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.ending_result),
            style = MaterialTheme.typography.bodyMedium,
            color = c.inkMuted,
        )
        val standing = when (val outcome = results.outcome) {
            is Outcome.Winner -> {
                val runnerUp = results.options.getOrNull(1)?.option?.voteCount ?: 0
                stringResource(R.string.end_leader, outcome.option.text, outcome.option.voteCount, runnerUp, voteCountLabel(results.totalVotes))
            }
            is Outcome.Tie -> pluralStringResource(R.plurals.tie_votes, outcome.options.first().voteCount, outcome.options.first().voteCount)
            Outcome.NoVotes -> stringResource(R.string.nobody_yet)
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
            text = stringResource(R.string.end_reveal),
            onClick = onEnd,
            style = PickleButtonStyle.Tomato,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        PickleButton(
            text = stringResource(R.string.keep_open),
            onClick = onKeepOpen,
            style = PickleButtonStyle.Outline,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PollNotFound(
    onBack: () -> Unit,
    onRefresh: () -> Unit,
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.poll_unavailable),
                    style = MaterialTheme.typography.titleMedium,
                    color = Pickle.colors.inkMuted,
                )
                PickleButton(text = stringResource(R.string.refresh), onClick = onRefresh)
            }
        }
    }
}

@Composable
private fun voteCountLabel(count: Int): String = pluralStringResource(R.plurals.vote_count, count, count)

@Preview(showBackground = true, backgroundColor = 0xFFF6EFE3)
@Composable
private fun PollContentPreview(
    @PreviewParameter(SamplePollProvider::class) poll: Poll,
) {
    PickleTheme {
        PollContent(
            state = PollUiState.from(poll, SamplePolls.CREATOR_ID, null),
            onBack = {},
            onShare = {},
            onVote = {},
            onSelectOption = {},
            onChangeVote = {},
            onKeepVote = {},
            onSuggest = {},
            onClearSuggestionStatus = {},
            onModerate = { _, _ -> },
            onClose = {},
        )
    }
}
