package com.shkurta.pickle.ui.notifications

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.shkurta.pickle.ui.components.ActionFrame
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onPollClick: (String) -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val action by viewModel.actionState.collectAsStateWithLifecycle()
    ActionFrame(action, viewModel::retry) {
        NotificationsContent(
            state = state,
            onBack = onBack,
            onMarkAllRead = { viewModel.markAllRead() },
            onNotificationClick = { notification ->
                viewModel.markRead(notification.id)
                onPollClick(notification.pollId)
            },
        )
    }
}

@Composable
private fun NotificationsContent(
    state: NotificationsUiState,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit,
    onNotificationClick: (PollNotification) -> Unit,
) {
    val notifications = state.notifications
    val grouped = remember(notifications) { notifications.groupBy { dayGroup(it.createdAt) } }

    Scaffold(
        containerColor = Pickle.colors.canvas,
        topBar = {
            NotificationsTopBar(
                hasUnread = state.hasUnread,
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
                            SectionLabel(text = stringResource(group.label))
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
                text = stringResource(R.string.notifications),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
        },
        navigationIcon = {
            CircleIconButton(
                iconRes = R.drawable.ic_arrow_back,
                contentDescription = stringResource(R.string.back),
                onClick = onBack,
            )
        },
        actions = {
            if (hasUnread) {
                TextButton(onClick = onMarkAllRead, enabled = com.shkurta.pickle.ui.components.LocalActionsEnabled.current) {
                    Text(
                        text = stringResource(R.string.mark_all_read),
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
            text = stringResource(R.string.no_notifications),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PollNotification.title(): String = when (type) {
    NotificationType.POLL_CREATED -> stringResource(R.string.poll_live)
    NotificationType.POLL_CLOSED -> stringResource(R.string.poll_closed)
    NotificationType.NEW_VOTES -> pluralStringResource(R.plurals.new_votes, detail?.toIntOrNull() ?: 0, detail?.toIntOrNull() ?: 0)
    NotificationType.SUGGESTION_RECEIVED -> stringResource(R.string.new_suggestion, detail.orEmpty())
    NotificationType.SUGGESTION_APPROVED -> stringResource(R.string.suggestion_approved, detail.orEmpty())
    NotificationType.SUGGESTION_REJECTED -> stringResource(R.string.suggestion_rejected, detail.orEmpty())
}

@Composable
private fun PollNotification.body(): String =
    if (type == NotificationType.POLL_CLOSED && detail != null) {
        // These prefixes are the stored notification format, including existing notifications.
        val outcome = when {
            detail == "No votes" -> stringResource(R.string.no_votes)
            detail.startsWith("Winner: ") -> stringResource(R.string.notification_winner, detail.removePrefix("Winner: "))
            detail.startsWith("Tie: ") -> stringResource(R.string.notification_tie, detail.removePrefix("Tie: "))
            else -> detail
        }
        stringResource(R.string.notification_result, outcome, pollQuestion)
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

private enum class DayGroup(val label: Int) {
    TODAY(R.string.today),
    YESTERDAY(R.string.yesterday),
    EARLIER(R.string.earlier),
}

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

@Composable
private fun relativeTime(instant: Instant, now: Instant = Instant.now()): String {
    val duration = Duration.between(instant, now)
    return when {
        duration.toDays() > 0 -> stringResource(R.string.relative_days, duration.toDays())
        duration.toHours() > 0 -> stringResource(R.string.relative_hours, duration.toHours())
        duration.toMinutes() > 0 -> stringResource(R.string.relative_minutes, duration.toMinutes())
        else -> stringResource(R.string.now)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6EFE3)
@Composable
private fun NotificationsContentPreview() {
    PickleTheme {
        NotificationsContent(
            state = NotificationsUiState(SampleNotifications.all),
            onBack = {},
            onMarkAllRead = {},
            onNotificationClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6EFE3)
@Composable
private fun NotificationsEmptyPreview() {
    PickleTheme {
        NotificationsContent(
            state = NotificationsUiState(),
            onBack = {},
            onMarkAllRead = {},
            onNotificationClick = {},
        )
    }
}
