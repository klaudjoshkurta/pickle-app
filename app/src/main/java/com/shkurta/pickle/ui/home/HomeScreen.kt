package com.shkurta.pickle.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.shkurta.pickle.R
import com.shkurta.pickle.data.NotificationRepository
import com.shkurta.pickle.data.PollRepository
import com.shkurta.pickle.ui.components.PollCard
import com.shkurta.pickle.ui.components.SegmentedToggle

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    onPollClick: (String) -> Unit = {},
    onCreateClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
) {
    var filter by rememberSaveable { mutableStateOf(HomeFilter.MY_POLLS) }
    val allPolls by PollRepository.Default.polls.collectAsState()
    val notifications by NotificationRepository.Default.notifications.collectAsState()
    val polls = remember(filter, allPolls) {
        allPolls.filter { poll ->
            when (filter) {
                HomeFilter.MY_POLLS -> poll.isOpen()
                HomeFilter.CLOSED -> !poll.isOpen()
            }
        }
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                hasUnreadNotifications = notifications.any { !it.isRead },
                onNotificationsClick = onNotificationsClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    hoveredElevation = 0.dp,
                    focusedElevation = 0.dp
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            SegmentedToggle(
                options = HomeFilter.entries,
                selected = filter,
                onSelect = { filter = it },
                label = { it.label },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
            )
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = polls,
                    key = { it.id }
                ) { poll ->
                    PollCard(
                        poll = poll,
                        onClick = { onPollClick(poll.id) }
                    )
                }
            }
        }
    }
}

private enum class HomeFilter(val label: String) {
    MY_POLLS("My polls"),
    CLOSED("Closed"),
}

@Composable
fun HomeTopBar(
    modifier: Modifier = Modifier,
    hasUnreadNotifications: Boolean = false,
    onNotificationsClick: () -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = {},
        actions = {
            IconButton(
                onClick = onNotificationsClick
            ) {
                BadgedBox(
                    badge = {
                        if (hasUnreadNotifications) {
                            Badge(containerColor = MaterialTheme.colorScheme.primary)
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_bell),
                        contentDescription = null
                    )
                }
            }
            IconButton(
                onClick = {}
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_menu),
                    contentDescription = null
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}