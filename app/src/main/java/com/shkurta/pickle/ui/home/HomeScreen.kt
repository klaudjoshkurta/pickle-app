package com.shkurta.pickle.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shkurta.pickle.R
import com.shkurta.pickle.domain.model.Outcome
import com.shkurta.pickle.domain.model.Poll
import com.shkurta.pickle.domain.model.PollStarter
import com.shkurta.pickle.domain.model.VotingMode
import com.shkurta.pickle.domain.model.toResults
import com.shkurta.pickle.ui.components.ActionFrame
import com.shkurta.pickle.ui.components.Avatar
import com.shkurta.pickle.ui.components.PickleBrand
import com.shkurta.pickle.ui.components.PickleButton
import com.shkurta.pickle.ui.components.PickleButtonStyle
import com.shkurta.pickle.ui.components.PickleCard
import com.shkurta.pickle.ui.components.PickleHeadline
import com.shkurta.pickle.ui.components.PollCard
import com.shkurta.pickle.ui.components.SegmentedToggle
import com.shkurta.pickle.ui.components.TemplateIcon
import com.shkurta.pickle.ui.components.dashedBorder
import com.shkurta.pickle.ui.components.localizedTitle
import com.shkurta.pickle.ui.components.resultCountLabel
import com.shkurta.pickle.ui.components.sharePoll
import com.shkurta.pickle.ui.theme.Pickle
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onPollClick: (String) -> Unit = {}, onCreateClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}, onMenuClick: () -> Unit = {},
    onTemplatesClick: () -> Unit = {}, onStarterClick: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val action by viewModel.actionState.collectAsStateWithLifecycle()

    ActionFrame(
        state = action,
        onRetry = viewModel::retry
    ) {
        HomeContent(
            state = state,
            onFilterChange = {
                if (it == HomeFilter.TEMPLATES) onTemplatesClick() else viewModel.selectFilter(it)
            },
            onPollClick = onPollClick,
            onCreateClick = onCreateClick,
            onMenuClick = onMenuClick,
            onStarterClick = onStarterClick,
            loadError = action.message
        )
    }
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onFilterChange: (HomeFilter) -> Unit,
    onPollClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    onMenuClick: () -> Unit,
    onStarterClick: (String) -> Unit,
    loadError: Int? = null
) {
    val firstRun = state.hasLoaded && !state.hasAnyPolls
    val context = LocalContext.current

    Scaffold(
        containerColor = Pickle.colors.canvas,
        topBar = {
            HomeTopBar(
                avatarUrl = state.avatarUrl,
                displayName = state.displayName,
                hasUnreadNotifications = state.hasUnreadNotifications,
                showCreate = !firstRun,
                onCreateClick = onCreateClick,
                onMenuClick = onMenuClick
            )
        },
        bottomBar = {
            if (firstRun) {
                PickleButton(
                    text = stringResource(R.string.start_first_poll),
                    onClick = onCreateClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    style = PickleButtonStyle.Teal, leadingIcon = R.drawable.ic_add
                )
            }
        }
    ) { padding ->
        if (!state.hasLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                if (loadError != null) {
                    Text(
                        text = stringResource(R.string.polls_load_error),
                        modifier = Modifier.padding(24.dp),
                        color = Pickle.colors.inkMuted,
                        textAlign = TextAlign.Center
                    )
                } else {
                    CircularProgressIndicator(color = Pickle.colors.teal)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.isDemo) {
                    item {
                        Text(
                            stringResource(R.string.offline_home),
                            style = MaterialTheme.typography.labelSmall,
                            color = Pickle.colors.inkMuted
                        )
                    }
                }
                if (firstRun) {
                    item {
                        val first = state.displayName?.trim()?.substringBefore(' ')?.takeIf { it.isNotEmpty() }
                        Text(
                            text = if (first == null) stringResource(R.string.first_poll_greeting) else stringResource(R.string.first_poll_greeting_name, first),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = Pickle.colors.ink
                        )
                    }
                    item { FirstPollSteps() }
                    item {
                        Text(
                            text = stringResource(R.string.start_template),
                            style = MaterialTheme.typography.labelLarge,
                            color = Pickle.colors.inkMuted
                        )
                    }
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (pair in listOf(listOf(PollStarter.PIZZA, PollStarter.FILM), listOf(PollStarter.DATE, PollStarter.PLACE))) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    pair.forEach { starter ->
                                        StarterChip(starter = starter, onClick = { onStarterClick(starter.id) })
                                    }
                                }
                            }
                            PickleButton(
                                text = stringResource(R.string.blank_poll),
                                onClick = onCreateClick,
                                style = PickleButtonStyle.Outline,
                                leadingIcon = R.drawable.ic_add,
                                height = 38.dp
                            )
                        }
                    }
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .dashedBorder(Pickle.colors.lineStrong, 18.dp)
                                .padding(vertical = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "✧",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Pickle.colors.inkMuted
                            )
                            Text(
                                text = stringResource(R.string.polls_live_here),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Pickle.colors.inkMuted
                            )
                            Text(
                                text = stringResource(R.string.live_float),
                                style = MaterialTheme.typography.bodySmall,
                                color = Pickle.colors.inkMuted
                            )
                        }
                    }
                } else {
                    item {
                        PickleHeadline(
                            text = greeting(state.displayName),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                    item {
                        SegmentedToggle(
                            options = HomeFilter.entries,
                            selected = state.filter,
                            onSelect = onFilterChange,
                            label = { stringResource(it.label) },
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        )
                    }
                    if (state.filter == HomeFilter.MY_POLLS) {
                        if (state.polls.isNotEmpty()) {
                            item {
                                HomeSectionLabel(stringResource(R.string.live_now))
                            }
                        }
                        items(
                            items = state.polls,
                            key = { it.id }
                        ) { poll ->
                            PollCard(
                                poll = poll,
                                showSuggestionBadge = !state.isDemo,
                                onClick = { onPollClick(poll.id) },
                                onShareClick = { if (!state.isDemo) sharePoll(context, poll) }
                            )
                        }
                        if (state.polls.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.no_live_polls),
                                    color = Pickle.colors.inkMuted,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        if (state.settledPolls.isNotEmpty()) {
                            item {
                                HomeSectionLabel(stringResource(R.string.settled))
                            }
                            item {
                                SettledPolls(state.settledPolls, onPollClick)
                            }
                        }
                    } else {
                        item {
                            HomeSectionLabel(stringResource(R.string.settled))
                        }
                        if (state.polls.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.no_settled_polls),
                                    color = Pickle.colors.inkMuted
                                )
                            }
                        }
                        else {
                            item { SettledPolls(state.polls, onPollClick) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSectionLabel(
    text: String
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = Pickle.colors.inkMuted
    )
}

@Composable
private fun FirstPollSteps() {
    PickleCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            listOf(
                stringResource(R.string.ask_question) to stringResource(R.string.question_examples),
                stringResource(R.string.add_options_step) to stringResource(R.string.template_step),
                stringResource(R.string.share_link_step) to stringResource(R.string.friends_vote_step)
            ).forEachIndexed { index, pair ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = if (index == 0) Pickle.colors.mustard else Pickle.colors.surface,
                        shape = CircleShape,
                        border = BorderStroke(1.5.dp, Pickle.colors.lineStrong)
                    ) {
                        Box(
                            modifier = Modifier.size(30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${index + 1}", fontWeight = FontWeight.Bold)
                        }
                    }
                    Column {
                        Text(
                            pair.first,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        ); Text(
                        pair.second,
                        style = MaterialTheme.typography.bodySmall,
                        color = Pickle.colors.inkMuted
                    )
                    }
                }
            }
        }
    }
}

@Composable
private fun StarterChip(starter: PollStarter, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Pickle.colors.surface,
        border = BorderStroke(1.5.dp, Pickle.colors.lineStrong)
    ) {
        Row(
            Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TemplateIcon(starter.icon, Modifier.size(14.dp))
            Text(
                if (starter == PollStarter.PIZZA) stringResource(R.string.pizza_night) else starter.localizedTitle(),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun SettledPolls(polls: List<Poll>, onClick: (String) -> Unit) {
    PickleCard(contentPadding = PaddingValues(vertical = 6.dp)) {
        polls.forEach { poll ->
            Surface(onClick = { onClick(poll.id) }, color = Pickle.colors.surface) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = Pickle.colors.mustard,
                        shape = CircleShape,
                        border = BorderStroke(1.5.dp, Pickle.colors.lineStrong)
                    ) {
                        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                painterResource(if (poll.toResults().outcome is Outcome.Winner) R.drawable.ic_trophy else R.drawable.ic_tie),
                                null,
                                Modifier.size(18.dp),
                                tint = Pickle.colors.ink
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            poll.question,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        val summary = when (val result = poll.toResults().outcome) {
                            is Outcome.Winner -> result.option.text + " · " + resultCountLabel(
                                result.option.voteCount,
                                poll.totalVotes,
                                poll.settings.votingMode == VotingMode.MULTIPLE
                            )

                            is Outcome.Tie -> stringResource(
                                R.string.tie_options,
                                result.options.joinToString(" & ") { it.text })

                            Outcome.NoVotes -> stringResource(R.string.nobody_voted)
                        }
                        Text(
                            summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = Pickle.colors.inkMuted
                        )
                    }
                    Text(
                        (poll.closedAt ?: poll.createdAt).atZone(ZoneId.systemDefault()).format(
                            DateTimeFormatter.ofPattern(
                                "EEE",
                                androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
                            )
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Pickle.colors.inkMuted
                    )
                }
            }
        }
    }
}

@Composable
fun HomeTopBar(
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    displayName: String? = null,
    hasUnreadNotifications: Boolean = false,
    showCreate: Boolean = true,
    onCreateClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    TopAppBar(
        modifier = modifier,
        title = {},
        actions = {
            if (showCreate) PickleButton(
            stringResource(R.string.new_poll),
            onCreateClick,
            style = PickleButtonStyle.Outline,
            leadingIcon = R.drawable.ic_add,
            height = 38.dp
        )
        IconButton(onClick = onMenuClick) {
            BadgedBox(badge = { if (hasUnreadNotifications) Badge(containerColor = Pickle.colors.tomato) }) {
                Avatar(
                    url = avatarUrl,
                    fallbackInitial = displayName,
                    size = 34.dp
                )
            }
        }
        Spacer(Modifier.width(8.dp))
    }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Pickle.colors.canvas))
}

@Composable
private fun greeting(name: String?): String {
    val time = when (LocalTime.now().hour) {
        in 5..11 -> stringResource(R.string.morning); in 12..17 -> stringResource(R.string.afternoon); else -> stringResource(
            R.string.evening
        )
    }
    val first = name?.trim()?.substringBefore(' ')?.takeIf { it.isNotEmpty() }
    return if (first == null) stringResource(R.string.your_polls) else stringResource(
        R.string.greeting_name,
        time,
        first
    )
}
