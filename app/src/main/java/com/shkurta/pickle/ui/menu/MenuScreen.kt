package com.shkurta.pickle.ui.menu

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shkurta.pickle.R
import com.shkurta.pickle.data.NotificationRepository
import com.shkurta.pickle.data.PollRepository
import com.shkurta.pickle.data.sample.SamplePolls
import com.shkurta.pickle.ui.theme.PickleTheme

private val CardShape = RoundedCornerShape(20.dp)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MenuScreen(
    onBack: () -> Unit,
    onCreatePollClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    pollRepository: PollRepository = PollRepository.Default,
    notificationRepository: NotificationRepository = NotificationRepository.Default,
) {
    val polls by pollRepository.polls.collectAsState()
    val notifications by notificationRepository.notifications.collectAsState()
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()
    }

    val myPolls = polls.filter { it.isCreator(SamplePolls.CREATOR_ID) }

    MenuContent(
        createdCount = myPolls.size,
        openCount = myPolls.count { it.isOpen() },
        voteCount = myPolls.sumOf { it.totalVotes },
        unreadCount = notifications.count { !it.isRead },
        versionName = versionName,
        onBack = onBack,
        onCreatePollClick = onCreatePollClick,
        onNotificationsClick = onNotificationsClick,
        onShareAppClick = { shareApp(context) },
    )
}

@Composable
private fun MenuContent(
    createdCount: Int,
    openCount: Int,
    voteCount: Int,
    unreadCount: Int,
    versionName: String?,
    onBack: () -> Unit,
    onCreatePollClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onShareAppClick: () -> Unit,
) {
    Scaffold(
        topBar = { MenuTopBar(onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = CardShape,
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 20.dp),
                ) {
                    Stat(value = createdCount, label = "Polls", modifier = Modifier.weight(1f))
                    Stat(value = openCount, label = "Open", modifier = Modifier.weight(1f))
                    Stat(value = voteCount, label = "Votes", modifier = Modifier.weight(1f))
                }
            }

            SectionLabel(text = "Polls")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = CardShape,
            ) {
                Column {
                    MenuRow(
                        iconRes = R.drawable.ic_add,
                        label = "New poll",
                        onClick = onCreatePollClick,
                    )
                    CardDivider()
                    MenuRow(
                        iconRes = R.drawable.ic_bell,
                        label = "Notifications",
                        trailing = if (unreadCount > 0) "$unreadCount new" else null,
                        highlightTrailing = true,
                        onClick = onNotificationsClick,
                    )
                }
            }

            SectionLabel(text = "About")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = CardShape,
            ) {
                Column {
                    MenuRow(
                        iconRes = R.drawable.ic_share,
                        label = "Share Pickle",
                        onClick = onShareAppClick,
                    )
                    if (versionName != null) {
                        CardDivider()
                        MenuRow(
                            iconRes = R.drawable.ic_check,
                            label = "Version",
                            trailing = versionName,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = "Menu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

@Composable
private fun Stat(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
        )
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
        modifier = modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun CardDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    )
}

@Composable
private fun MenuRow(
    iconRes: Int,
    label: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    highlightTrailing: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (highlightTrailing) FontWeight.SemiBold else FontWeight.Normal,
                color = if (highlightTrailing) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
                },
            )
        }
    }
}

private fun shareApp(context: Context) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Make group decisions with Pickle: https://pickle.app")
    }
    context.startActivity(Intent.createChooser(intent, null))
}

@Preview(showBackground = true, backgroundColor = 0xFFFAF2E3)
@Composable
private fun MenuContentPreview() {
    PickleTheme {
        MenuContent(
            createdCount = 3,
            openCount = 2,
            voteCount = 19,
            unreadCount = 2,
            versionName = "1.0",
            onBack = {},
            onCreatePollClick = {},
            onNotificationsClick = {},
            onShareAppClick = {},
        )
    }
}
