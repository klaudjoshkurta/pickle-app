package com.shkurta.pickle.ui.create

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.shkurta.pickle.R
import com.shkurta.pickle.data.MIN_POLL_OPTIONS
import com.shkurta.pickle.data.distinctOptionTexts
import com.shkurta.pickle.domain.model.PollSettings
import com.shkurta.pickle.ui.theme.PickleTheme
import java.time.Instant
import java.time.temporal.ChronoUnit

private const val MAX_POLL_OPTIONS = 10
private val CardShape = RoundedCornerShape(20.dp)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CreatePollScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: CreatePollViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.createdPollId.collect { pollId ->
            onCreated(pollId)
        }
    }

    CreatePollContent(
        onBack = onBack,
        onCreate = { question, options, settings, deadline, expectedVoters ->
            viewModel.createPoll(
                question = question,
                optionTexts = options,
                settings = settings,
                closesAt = deadline.hours?.let { Instant.now().plus(it, ChronoUnit.HOURS) },
                expectedVoters = expectedVoters,
            )
        },
    )
}

@Composable
private fun CreatePollContent(
    onBack: () -> Unit,
    onCreate: (
        question: String,
        options: List<String>,
        settings: PollSettings,
        deadline: Deadline,
        expectedVoters: Int?,
    ) -> Unit,
) {
    val defaults = PollSettings()
    var question by rememberSaveable { mutableStateOf("") }
    var options by rememberSaveable { mutableStateOf(List(MIN_POLL_OPTIONS) { "" }) }
    var allowSuggestions by rememberSaveable { mutableStateOf(defaults.allowSuggestions) }
    var requireApproval by rememberSaveable { mutableStateOf(defaults.requireApproval) }
    var showResultsBeforeVoting by rememberSaveable { mutableStateOf(defaults.showResultsBeforeVoting) }
    var allowVoteChange by rememberSaveable { mutableStateOf(defaults.allowVoteChange) }
    var deadline by rememberSaveable { mutableStateOf(Deadline.NONE) }
    var expectedVoters by rememberSaveable { mutableStateOf("") }

    val isValid = question.isNotBlank() && distinctOptionTexts(options).size >= MIN_POLL_OPTIONS

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = { CreatePollTopBar(onBack = onBack) },
        bottomBar = {
            Button(
                onClick = {
                    onCreate(
                        question,
                        options,
                        PollSettings(
                            allowSuggestions = allowSuggestions,
                            requireApproval = requireApproval,
                            showResultsBeforeVoting = showResultsBeforeVoting,
                            allowVoteChange = allowVoteChange,
                        ),
                        deadline,
                        expectedVoters.toIntOrNull(),
                    )
                },
                enabled = isValid,
                modifier = Modifier
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(52.dp),
            ) {
                Text(
                    text = "Create poll",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
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
                TextField(
                    value = question,
                    onValueChange = { question = it },
                    placeholder = {
                        Text(
                            text = "What do you want to ask?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    minLines = 2,
                    colors = plainFieldColors(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                )
            }

            SectionLabel(text = "Options")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = CardShape,
            ) {
                Column {
                    options.forEachIndexed { index, text ->
                        OptionField(
                            number = index + 1,
                            text = text,
                            onTextChange = { newText ->
                                options = options.toMutableList().also { it[index] = newText }
                            },
                            canRemove = options.size > MIN_POLL_OPTIONS,
                            onRemove = {
                                options = options.toMutableList().also { it.removeAt(index) }
                            },
                        )
                        CardDivider()
                    }
                    if (options.size < MAX_POLL_OPTIONS) {
                        AddOptionRow(onClick = { options = options + "" })
                    } else {
                        Text(
                            text = "That's the maximum of $MAX_POLL_OPTIONS options",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                        )
                    }
                }
            }

            SectionLabel(text = "Closes")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Deadline.entries.forEach { entry ->
                    DeadlinePill(
                        label = entry.label,
                        selected = entry == deadline,
                        onClick = { deadline = entry },
                    )
                }
            }

            SectionLabel(text = "Settings")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = CardShape,
            ) {
                Column {
                    SettingRow(
                        label = "Allow suggestions",
                        description = "Voters can add their own options",
                        checked = allowSuggestions,
                        onCheckedChange = { allowSuggestions = it },
                    )
                    AnimatedVisibility(visible = allowSuggestions) {
                        Column {
                            CardDivider()
                            SettingRow(
                                label = "Approve suggestions first",
                                description = "New options wait for your OK",
                                checked = requireApproval,
                                onCheckedChange = { requireApproval = it },
                            )
                        }
                    }
                    CardDivider()
                    SettingRow(
                        label = "Show results before voting",
                        description = "Everyone sees the count right away",
                        checked = showResultsBeforeVoting,
                        onCheckedChange = { showResultsBeforeVoting = it },
                    )
                    CardDivider()
                    SettingRow(
                        label = "Allow changing vote",
                        description = "Voters can switch while the poll is open",
                        checked = allowVoteChange,
                        onCheckedChange = { allowVoteChange = it },
                    )
                    CardDivider()
                    ExpectedVotersRow(
                        value = expectedVoters,
                        onValueChange = { expectedVoters = it.filter(Char::isDigit).take(4) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CreatePollTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = "New poll",
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
private fun OptionField(
    number: Int,
    text: String,
    onTextChange: (String) -> Unit,
    canRemove: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("Option $number") },
            singleLine = true,
            colors = plainFieldColors(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.weight(1f),
        )
        if (canRemove) {
            IconButton(onClick = onRemove) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Remove option $number",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
    }
}

@Composable
private fun AddOptionRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = "Add option",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun DeadlinePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
        )
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
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun ExpectedVotersRow(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier
                .width(64.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.Center) {
                    if (value.isEmpty()) {
                        Text(
                            text = "–",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f),
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
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
        )
    }
}

@Composable
private fun plainFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
)

private enum class Deadline(val label: String, val hours: Long?) {
    NONE("No deadline", null),
    ONE_HOUR("1 hour", 1),
    ONE_DAY("1 day", 24),
    THREE_DAYS("3 days", 72),
    ONE_WEEK("1 week", 168),
}

@Preview(showBackground = true, backgroundColor = 0xFFFAF2E3)
@Composable
private fun CreatePollContentPreview() {
    PickleTheme {
        CreatePollContent(
            onBack = {},
            onCreate = { _, _, _, _, _ -> },
        )
    }
}
