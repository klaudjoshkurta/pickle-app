package com.shkurta.pickle.ui.menu

import androidx.compose.ui.res.stringResource
import android.content.Context
import androidx.compose.ui.res.pluralStringResource
import android.content.Intent
import com.shkurta.pickle.ui.components.ActionFrame
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.shkurta.pickle.R
import com.shkurta.pickle.ui.components.Avatar
import com.shkurta.pickle.ui.components.CircleIconButton
import com.shkurta.pickle.ui.components.PickleBorderWidth
import com.shkurta.pickle.ui.components.PickleCard
import com.shkurta.pickle.ui.components.PickleHeadline
import com.shkurta.pickle.ui.theme.Pickle
import com.shkurta.pickle.ui.theme.PickleTheme


@Composable
fun MenuScreen(
    onBack: () -> Unit,
    onCreatePollClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSignedOut: () -> Unit = {},
    viewModel: MenuViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val signedOut by viewModel.signedOut.collectAsStateWithLifecycle()
    LaunchedEffect(signedOut) { if (signedOut) onSignedOut() }
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()
    }

    val action by viewModel.actionState.collectAsStateWithLifecycle()
    ActionFrame(action, viewModel::retry) {
        MenuContent(
            displayName = state.displayName,
            avatarUrl = state.avatarUrl,
            createdCount = state.createdCount,
            openCount = state.openCount,
            voteCount = state.voteCount,
            unreadCount = state.unreadCount,
            versionName = versionName,
            onBack = onBack,
            onCreatePollClick = onCreatePollClick,
            onNotificationsClick = onNotificationsClick,
            onShareAppClick = { shareApp(context) },
            onSignOut = { viewModel.signOut() },
        )
    }
}

@Composable
private fun MenuContent(
    displayName: String?,
    avatarUrl: String?,
    createdCount: Int,
    openCount: Int,
    voteCount: Int,
    unreadCount: Int,
    versionName: String?,
    onBack: () -> Unit,
    onCreatePollClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onShareAppClick: () -> Unit,
    onSignOut: () -> Unit = {},
) {
    Scaffold(
        containerColor = Pickle.colors.canvas,
        topBar = { MenuTopBar(onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Avatar(
                    url = avatarUrl,
                    fallbackInitial = displayName,
                    size = 96.dp,
                    modifier = Modifier.border(4.dp, Pickle.colors.mustard, CircleShape),
                )
                if (displayName != null) {
                    Spacer(Modifier.height(12.dp))
                    PickleHeadline(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.profile_description),
                    style = MaterialTheme.typography.labelMedium,
                    color = Pickle.colors.inkMuted,
                )
            }
            Spacer(Modifier.height(20.dp))

            PickleCard(contentPadding = PaddingValues(vertical = 18.dp)) {
                Row {
                    Stat(value = createdCount, label = stringResource(R.string.polls), modifier = Modifier.weight(1f))
                    Stat(value = openCount, label = stringResource(R.string.live), modifier = Modifier.weight(1f))
                    Stat(value = voteCount, label = stringResource(R.string.votes_label), modifier = Modifier.weight(1f))
                }
            }

            SectionLabel(text = stringResource(R.string.polls))
            PickleCard(contentPadding = PaddingValues(0.dp)) {
                Column {
                    MenuRow(
                        iconRes = R.drawable.ic_add,
                        label = stringResource(R.string.new_poll),
                        onClick = onCreatePollClick,
                    )
                    CardDivider()
                    MenuRow(
                        iconRes = R.drawable.ic_bell,
                        label = stringResource(R.string.notifications),
                        trailing = if (unreadCount > 0) pluralStringResource(R.plurals.unread_count, unreadCount, unreadCount) else null,
                        highlightTrailing = true,
                        onClick = onNotificationsClick,
                    )
                }
            }

            SectionLabel(text = stringResource(R.string.about))
            PickleCard(contentPadding = PaddingValues(0.dp)) {
                Column {
                    MenuRow(
                        iconRes = R.drawable.ic_share,
                        label = stringResource(R.string.share_pickle),
                        onClick = onShareAppClick,
                    )
                    if (versionName != null) {
                        CardDivider()
                        MenuRow(
                            iconRes = R.drawable.ic_check,
                            label = stringResource(R.string.version),
                            trailing = versionName,
                        )
                    }
                    CardDivider()
                    MenuRow(
                        iconRes = R.drawable.ic_close,
                        label = stringResource(R.string.sign_out),
                        onClick = onSignOut,
                    )
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
        modifier = modifier.padding(horizontal = 8.dp),
        title = {
            Text(
                text = stringResource(R.string.account),
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
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Pickle.colors.canvas,
            titleContentColor = Pickle.colors.ink,
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
            style = MaterialTheme.typography.headlineMedium,
            color = Pickle.colors.ink,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Pickle.colors.inkMuted,
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
        color = Pickle.colors.inkMuted,
        modifier = modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun CardDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = Pickle.colors.lineSoft,
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
                    Modifier.clickable(enabled = com.shkurta.pickle.ui.components.LocalActionsEnabled.current, role = Role.Button, onClick = onClick)
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
                .background(Pickle.colors.tealSoft, CircleShape)
                .border(PickleBorderWidth, Pickle.colors.lineStrong, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Pickle.colors.ink,
                modifier = Modifier.size(18.dp),
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
                color = if (highlightTrailing) Pickle.colors.textAccent else Pickle.colors.inkMuted,
            )
        }
    }
}

private fun shareApp(context: Context) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, context.getString(com.shkurta.pickle.R.string.share_app, com.shkurta.pickle.ui.components.WEB_BASE_URL))
    }
    context.startActivity(Intent.createChooser(intent, null))
}

@Preview(showBackground = true, backgroundColor = 0xFFF6EFE3)
@Composable
private fun MenuContentPreview() {
    PickleTheme {
        MenuContent(
            displayName = "Klaud",
            avatarUrl = null,
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
