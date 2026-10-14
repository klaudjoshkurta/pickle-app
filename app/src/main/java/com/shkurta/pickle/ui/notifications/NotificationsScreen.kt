package com.shkurta.pickle.ui.notifications

import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.shkurta.pickle.R
import com.shkurta.pickle.data.sample.SampleNotifications
import com.shkurta.pickle.domain.model.NotificationType
import com.shkurta.pickle.domain.model.PollNotification
import com.shkurta.pickle.ui.components.CircleIconButton
import com.shkurta.pickle.ui.components.PickleBorderWidth
import com.shkurta.pickle.ui.components.PickleCard
import com.shkurta.pickle.ui.theme.Pickle
import com.shkurta.pickle.ui.theme.PickleTheme
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onPollClick: (String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val notifications by viewModel.notifications.collectAsState()

    NotificationsContent(
        notifications = notifications,
        onBack = onBack,
        onMarkAllRead = { viewModel.markAllRead() },
        onNotificationClick = { notification ->
            viewModel.markRead(notification.id)
            onPollClick(notification.pollId)
        },
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun NotificationsContent(
    notifications: List<PollNotification>,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit,
    onNotificationClick: (PollNotification) -> Unit,
) {
    val grouped = remember(notifications) { notifications.groupBy { dayGroup(it.createdAt) } }

    Scaffold(
        containerColor = Pickle.colors.canvas,
        topBar = {
            NotificationsTopBar(
                hasUnread = notifications.any { !it.isRead },
                onBack = onBack,
                onMarkAllRead = onMarkAllRead,
            )
        },
    ) { innerPadding ->
        if (notifications.isEmpty()) {
            EmptyNotifications(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DayGroup.entries.forEach { group ->
                    val groupNotifications = grouped[group].orEmpty()
                    if (groupNotifications.isNotEmpty()) {
                        item(key = "label_${group.name}") {
                            SectionLabel(text = group.label)
                        }
                        items(
                            items = groupNotifications,
                            key = { it.id },
                        ) { notification ->
                            NotificationRow(
                                notification = notification,
                                onClick = { onNotificationClick(notification) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationsTopBar(
    hasUnread: Boolean,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier.padding(horizontal = 8.dp),
        title = {
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
        },
        navigationIcon = {
            CircleIconButton(
                iconRes = R.drawable.ic_arrow_back,
                contentDescription = "Back",
                onClick = onBack,
            )
        },
        actions = {
            if (hasUnread) {
                TextButton(onClick = onMarkAllRead) {
                    Text(
                        text = "Mark all read",
                        style = MaterialTheme.typography.labelLarge,
                        color = Pickle.colors.textAccent,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Pickle.colors.canvas,
            titleContentColor = Pickle.colors.ink,
        ),
    )
}

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = Pickle.colors.inkMuted,
        modifier = modifier.padding(start = 4.dp, top = 14.dp),
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun NotificationRow(
    notification: PollNotification,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Pickle.colors
    val (badgeColor, iconColor) = notification.type.badgeColors()

    PickleCard(
        modifier = modifier,
        onClick = onClick,
        borderColor = if (notification.isRead) c.lineSoft else c.lineStrong,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(badgeColor, CircleShape)
                    .border(PickleBorderWidth, c.lineStrong, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(notification.type.iconRes()),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = notification.body(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.inkMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = relativeTime(notification.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = c.inkMuted,
                )
                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(c.tomato, CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyNotifications(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_bell),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f),
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "No notifications yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun PollNotification.title(): String = when (type) {
    NotificationType.POLL_CREATED -> "Your poll is live"
    NotificationType.POLL_CLOSED -> "Poll closed"
    NotificationType.NEW_VOTES -> if (detail == "1") "1 new vote" else "$detail new votes"
    NotificationType.SUGGESTION_RECEIVED -> "New suggestion: $detail"
    NotificationType.SUGGESTION_APPROVED -> "Suggestion approved: $detail"
    NotificationType.SUGGESTION_REJECTED -> "Suggestion rejected: $detail"
}

private fun PollNotification.body(): String =
    if (type == NotificationType.POLL_CLOSED && detail != null) {
        "$detail · $pollQuestion"
    } else {
        pollQuestion
    }

private fun NotificationType.iconRes(): Int = when (this) {
    NotificationType.POLL_CREATED -> R.drawable.ic_add
    NotificationType.POLL_CLOSED -> R.drawable.ic_lock
    NotificationType.NEW_VOTES -> R.drawable.ic_chart
    NotificationType.SUGGESTION_RECEIVED -> R.drawable.ic_lightbulb
    NotificationType.SUGGESTION_APPROVED -> R.drawable.ic_check
    NotificationType.SUGGESTION_REJECTED -> R.drawable.ic_close
}

/** Badge fill to icon tint, following the brand roles (mustard = celebration, tomato = the leader, teal = live). */
@Composable
private fun NotificationType.badgeColors(): Pair<Color, Color> {
    val c = Pickle.colors
    return when (this) {
        NotificationType.POLL_CREATED -> c.teal to c.onBrand
        NotificationType.POLL_CLOSED -> c.mustard to c.onMustard
        NotificationType.NEW_VOTES -> c.tomato to c.onBrand
        NotificationType.SUGGESTION_RECEIVED -> c.tealSoft to c.ink
        NotificationType.SUGGESTION_APPROVED -> c.tealSoft to c.teal
        NotificationType.SUGGESTION_REJECTED -> c.sunken to c.inkMuted
    }
}

private enum class DayGroup(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    EARLIER("Earlier"),
}

@RequiresApi(Build.VERSION_CODES.O)
private fun dayGroup(
    instant: Instant,
    zone: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(zone),
): DayGroup {
    val date = instant.atZone(zone).toLocalDate()
    return when {
        !date.isBefore(today) -> DayGroup.TODAY
        date == today.minusDays(1) -> DayGroup.YESTERDAY
        else -> DayGroup.EARLIER
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun relativeTime(instant: Instant, now: Instant = Instant.now()): String {
    val duration = Duration.between(instant, now)
    return when {
        duration.toDays() > 0 -> "${duration.toDays()}d"
        duration.toHours() > 0 -> "${duration.toHours()}h"
        duration.toMinutes() > 0 -> "${duration.toMinutes()}m"
        else -> "now"
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, backgroundColor = 0xFFF6EFE3)
@Composable
private fun NotificationsContentPreview() {
    PickleTheme {
        NotificationsContent(
            notifications = SampleNotifications.all,
            onBack = {},
            onMarkAllRead = {},
            onNotificationClick = {},
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, backgroundColor = 0xFFF6EFE3)
@Composable
private fun NotificationsEmptyPreview() {
    PickleTheme {
        NotificationsContent(
            notifications = emptyList(),
            onBack = {},
            onMarkAllRead = {},
            onNotificationClick = {},
        )
    }
}
