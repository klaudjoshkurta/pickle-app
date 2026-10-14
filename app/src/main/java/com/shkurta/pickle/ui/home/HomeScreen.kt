package com.shkurta.pickle.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.shkurta.pickle.R
import com.shkurta.pickle.ui.components.Avatar
import com.shkurta.pickle.ui.components.PickleBrand
import com.shkurta.pickle.ui.components.PickleButton
import com.shkurta.pickle.ui.components.PickleButtonStyle
import com.shkurta.pickle.ui.components.PickleHeadline
import com.shkurta.pickle.ui.components.PollCard
import com.shkurta.pickle.ui.components.SectionLabel
import com.shkurta.pickle.ui.components.SegmentedToggle
import com.shkurta.pickle.ui.components.dashedBorder
import com.shkurta.pickle.ui.components.sharePoll
import com.shkurta.pickle.ui.theme.Pickle
import java.time.LocalTime

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    onPollClick: (String) -> Unit = {},
    onCreateClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    var filter by rememberSaveable { mutableStateOf(HomeFilter.MY_POLLS) }
    val allPolls by viewModel.polls.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val context = LocalContext.current
    val polls = remember(filter, allPolls) {
        allPolls.filter { poll ->
            when (filter) {
                HomeFilter.MY_POLLS -> poll.isOpen()
                HomeFilter.CLOSED -> !poll.isOpen()
            }
        }
    }

    Scaffold(
        containerColor = Pickle.colors.canvas,
        topBar = {
            HomeTopBar(
                avatarUrl = viewModel.avatarUrl,
                displayName = viewModel.displayName,
                hasUnreadNotifications = notifications.any { !it.isRead },
                onCreateClick = onCreateClick,
                onNotificationsClick = onNotificationsClick,
                onMenuClick = onMenuClick,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "greeting") {
                Column {
                    PickleHeadline(text = greeting(viewModel.displayName))
                    Spacer(Modifier.height(14.dp))
                    SegmentedToggle(
                        options = HomeFilter.entries,
                        selected = filter,
                        onSelect = { filter = it },
                        label = { it.label },
                    )
                    if (polls.isNotEmpty()) {
                        SectionLabel(text = filter.sectionLabel)
                    }
                }
            }

            if (polls.isEmpty()) {
                item(key = "empty") {
                    EmptyPolls(
                        filter = filter,
                        onCreateClick = onCreateClick,
                    )
                }
            }

            items(
                items = polls,
                key = { it.id }
            ) { poll ->
                PollCard(
                    poll = poll,
                    onClick = { onPollClick(poll.id) },
                    onShareClick = { sharePoll(context, poll) },
                )
            }
        }
    }
}

private enum class HomeFilter(val label: String, val sectionLabel: String) {
    MY_POLLS("My polls", "Live now"),
    CLOSED("Closed", "Settled"),
}

@RequiresApi(Build.VERSION_CODES.O)
private fun greeting(name: String?): String {
    val hour = LocalTime.now().hour
    val partOfDay = when (hour) {
        in 5..11 -> "Morning"
        in 12..17 -> "Afternoon"
        else -> "Evening"
    }
    val first = name?.trim()?.substringBefore(' ')?.takeIf { it.isNotEmpty() }
    return if (first != null) "$partOfDay, $first" else "Your polls"
}

@Composable
private fun EmptyPolls(
    filter: HomeFilter,
    onCreateClick: () -> Unit,
) {
    val c = Pickle.colors
    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .dashedBorder(c.lineStrong, 16.dp)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (filter == HomeFilter.MY_POLLS) "Your polls will live here" else "Nothing settled yet",
                style = MaterialTheme.typography.titleSmall,
                color = c.ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (filter == HomeFilter.MY_POLLS) "Live ones float to the top."
                else "Closed polls and their winners land here.",
                style = MaterialTheme.typography.bodySmall,
                color = c.inkMuted,
                textAlign = TextAlign.Center,
            )
        }
        if (filter == HomeFilter.MY_POLLS) {
            PickleButton(
                text = "Start your first poll",
                onClick = onCreateClick,
                style = PickleButtonStyle.Teal,
                leadingIcon = R.drawable.ic_add,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun HomeTopBar(
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    displayName: String? = null,
    hasUnreadNotifications: Boolean = false,
    onCreateClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
) {
    val c = Pickle.colors
    TopAppBar(
        modifier = modifier,
        title = { PickleBrand() },
        actions = {
            PickleButton(
                text = "New poll",
                onClick = onCreateClick,
                style = PickleButtonStyle.Outline,
                leadingIcon = R.drawable.ic_add,
                height = 36.dp,
            )
            IconButton(onClick = onNotificationsClick) {
                BadgedBox(
                    badge = {
                        if (hasUnreadNotifications) {
                            Badge(containerColor = c.tomato)
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_bell),
                        contentDescription = "Notifications",
                        tint = c.ink,
                    )
                }
            }
            IconButton(onClick = onMenuClick) {
                Avatar(
                    url = avatarUrl,
                    fallbackInitial = displayName,
                    size = 32.dp,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = c.canvas,
            titleContentColor = c.ink,
        )
    )
}
