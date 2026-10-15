package com.shkurta.pickle.ui.create

import com.shkurta.pickle.ui.components.ActionFrame
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.shkurta.pickle.R
import com.shkurta.pickle.domain.model.MIN_POLL_OPTIONS
import com.shkurta.pickle.domain.model.PollSettings
import com.shkurta.pickle.ui.components.CircleIconButton
import com.shkurta.pickle.ui.components.DashedActionButton
import com.shkurta.pickle.ui.components.PickleBorderWidth
import com.shkurta.pickle.ui.components.PickleButton
import com.shkurta.pickle.ui.components.PickleButtonStyle
import com.shkurta.pickle.ui.components.PickleCard
import com.shkurta.pickle.ui.components.PickleTextField
import com.shkurta.pickle.ui.components.pickleSwitchColors
import com.shkurta.pickle.ui.theme.Pickle
import com.shkurta.pickle.ui.theme.PickleTheme

@Composable
fun CreatePollScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: CreatePollViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.createdPollId.collect { pollId ->
            onCreated(pollId)
        }
    }

    val action by viewModel.actionState.collectAsStateWithLifecycle()
    ActionFrame(action, viewModel::retry) {
        CreatePollContent(
            state = state,
            onBack = onBack,
            onCreate = viewModel::createPoll,
            onQuestionChange = viewModel::setQuestion,
            onOptionChange = viewModel::setOption,
            onAddOption = viewModel::addOption,
            onRemoveOption = viewModel::removeOption,
            onSettingsChange = viewModel::setSettings,
            onDeadlineChange = viewModel::setDeadline,
            onExpectedVotersChange = viewModel::setExpectedVoters,
        )
    }
}

@Composable
private fun CreatePollContent(
    state: CreatePollUiState,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onQuestionChange: (String) -> Unit = {},
    onOptionChange: (Int, String) -> Unit = { _, _ -> },
    onAddOption: () -> Unit = {},
    onRemoveOption: (Int) -> Unit = {},
    onSettingsChange: (PollSettings) -> Unit = {},
    onDeadlineChange: (Deadline) -> Unit = {},
    onExpectedVotersChange: (String) -> Unit = {},
) {
    val c = Pickle.colors
    val question = state.question
    val options = state.options
    val allowSuggestions = state.settings.allowSuggestions
    val requireApproval = state.settings.requireApproval
    val showResultsBeforeVoting = state.settings.showResultsBeforeVoting
    val allowVoteChange = state.settings.allowVoteChange
    val deadline = state.deadline
    val expectedVoters = state.expectedVoters
    val filledOptions = state.filledOptions
    val isValid = state.isValid

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = c.canvas,
        topBar = { CreatePollTopBar(onBack = onBack) },
        bottomBar = {
            PickleButton(
                text = "Create & get the link",
                onClick = onCreate,
                enabled = isValid,
                style = PickleButtonStyle.Teal,
                modifier = Modifier
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        ) {
            SectionLabel(text = "What are we deciding?")
            PickleTextField(
                value = question,
                onValueChange = onQuestionChange,
                placeholder = "Pizza night: what are we ordering?",
                singleLine = false,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
            )

            SectionLabel(
                text = "Options",
                trailing = "$filledOptions of $MAX_POLL_OPTIONS",
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEachIndexed { index, text ->
                    PickleTextField(
                        value = text,
                        onValueChange = { newText -> onOptionChange(index, newText) },
                        placeholder = "Option ${index + 1}",
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next,
                        ),
                        trailing = if (options.size > MIN_POLL_OPTIONS) {
                            {
                                IconButton(
                                    onClick = { onRemoveOption(index) },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_close),
                                        contentDescription = "Remove option ${index + 1}",
                                        tint = c.inkMuted,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        } else {
                            null
                        },
                    )
                }
                DashedActionButton(
                    text = "Add an option",
                    onClick = onAddOption,
                    enabled = options.size < MAX_POLL_OPTIONS,
                )
            }
            Text(
                text = "Need at least $MIN_POLL_OPTIONS, up to $MAX_POLL_OPTIONS.",
                style = MaterialTheme.typography.labelSmall,
                color = c.inkMuted,
                modifier = Modifier.padding(start = 2.dp, top = 8.dp),
            )

            SectionLabel(text = "When does voting close?")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Deadline.entries.forEach { entry ->
                    DeadlinePill(
                        label = entry.label,
                        selected = entry == deadline,
                        onClick = { onDeadlineChange(entry) },
                    )
                }
            }

            SectionLabel(text = "How do people vote?")
            PickleCard(contentPadding = PaddingValues(0.dp)) {
                SettingRow(
                    label = "Voters can suggest options",
                    description = "Let the crew add ideas",
                    checked = allowSuggestions,
                    onCheckedChange = { onSettingsChange(state.settings.copy(allowSuggestions = it)) },
                )
                AnimatedVisibility(visible = allowSuggestions) {
                    Column {
                        CardDivider()
                        SettingRow(
                            label = "Approve suggestions first",
                            description = "You approve each one before it joins",
                            checked = requireApproval,
                            onCheckedChange = { onSettingsChange(state.settings.copy(requireApproval = it)) },
                        )
                    }
                }
                CardDivider()
                SettingRow(
                    label = "Show results before voting",
                    description = "Off = results only after you vote, no bandwagons",
                    checked = showResultsBeforeVoting,
                    onCheckedChange = { onSettingsChange(state.settings.copy(showResultsBeforeVoting = it)) },
                )
                CardDivider()
                SettingRow(
                    label = "Allow changing vote",
                    description = "Voters can switch while the poll is open",
                    checked = allowVoteChange,
                    onCheckedChange = { onSettingsChange(state.settings.copy(allowVoteChange = it)) },
                )
                CardDivider()
                ExpectedVotersRow(
                    value = expectedVoters,
                    onValueChange = onExpectedVotersChange,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePollTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier.padding(horizontal = 8.dp),
        title = {
            Text(
                text = "New poll",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
        },
        navigationIcon = {
            CircleIconButton(
                iconRes = R.drawable.ic_close,
                contentDescription = "Cancel",
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
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 2.dp, end = 2.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = Pickle.colors.ink,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium,
                color = Pickle.colors.inkMuted,
            )
        }
    }
}

@Composable
private fun CardDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = Pickle.colors.lineSoft,
    )
}

@Composable
private fun DeadlinePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Pickle.colors
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = if (selected) c.teal else c.surface,
        contentColor = if (selected) c.onBrand else c.ink,
        border = BorderStroke(PickleBorderWidth, c.lineStrong),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingText(
            label = label,
            description = description,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = pickleSwitchColors(),
        )
    }
}

@Composable
private fun ExpectedVotersRow(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Pickle.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingText(
            label = "Expected voters",
            description = "Optional, shows turnout on the poll",
            modifier = Modifier.weight(1f),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = c.ink,
            ),
            cursorBrush = SolidColor(c.teal),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier
                .width(64.dp)
                .background(c.surface, shape)
                .border(PickleBorderWidth, c.lineStrong, shape)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.Center) {
                    if (value.isEmpty()) {
                        Text(
                            text = "–",
                            style = MaterialTheme.typography.titleMedium,
                            color = c.inkMuted,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun SettingText(
    label: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Pickle.colors.ink,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
            color = Pickle.colors.inkMuted,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6EFE3)
@Composable
private fun CreatePollContentPreview() {
    PickleTheme {
        CreatePollContent(
            state = CreatePollUiState(),
            onBack = {},
            onCreate = {},
        )
    }
}
