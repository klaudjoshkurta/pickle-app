package com.shkurta.pickle.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.svg.SvgDecoder
import com.shkurta.pickle.R
import com.shkurta.pickle.ui.components.CircleIconButton
import com.shkurta.pickle.ui.components.PickleBorderWidth
import com.shkurta.pickle.ui.components.PickleBrand
import com.shkurta.pickle.ui.components.PickleButton
import com.shkurta.pickle.ui.components.PickleButtonStyle
import com.shkurta.pickle.ui.components.PickleHeadline
import com.shkurta.pickle.ui.components.PickleTextField
import com.shkurta.pickle.ui.theme.Pickle

@Composable
fun AuthScreen(
    initialSignUp: Boolean = false,
    onBack: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(initialSignUp) { viewModel.initialize(initialSignUp) }
    AuthContent(
        state = state,
        onBack = onBack,
        onNameChange = viewModel::setName,
        onEmailChange = viewModel::setEmail,
        onPasswordChange = viewModel::setPassword,
        onSelectAvatar = viewModel::selectAvatar,
        onShuffleAvatars = viewModel::shuffleAvatars,
        onToggleMode = viewModel::toggleMode,
        onPreviousStep = viewModel::previousStep,
        onNextStep = viewModel::nextStep,
        onSubmit = viewModel::submit,
        onClearError = viewModel::clearError,
    )
}

@Composable
private fun AuthContent(
    state: AuthUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSelectAvatar: (String) -> Unit,
    onShuffleAvatars: () -> Unit,
    onToggleMode: () -> Unit,
    onPreviousStep: () -> Unit,
    onNextStep: () -> Unit,
    onSubmit: () -> Unit,
    onClearError: () -> Unit,
) {
    val isSignUp = state.isSignUp
    val step = state.step
    val name = state.name
    val avatarSeeds = state.avatarSeeds
    val selectedSeed = state.selectedSeed
    val email = state.email
    val password = state.password
    val snackbarHostState = remember { SnackbarHostState() }
    val isLoading = state.isLoading

    LaunchedEffect(state.status) {
        val status = state.status
        if (status is AuthStatus.Error) {
            snackbarHostState.showSnackbar(status.message)
            onClearError()
        }
    }

    val isStepValid = state.isStepValid
    val canSignIn = state.canSignIn
    val submit = onSubmit
    val goNext = onNextStep
    val canGoBackStep = state.canGoBackStep
    val handleBack = {
        if (canGoBackStep) onPreviousStep()
        else onBack()
    }

    BackHandler(enabled = canGoBackStep && !isLoading) { handleBack() }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(horizontal = 12.dp),
                title = {},
                navigationIcon = {
                    CircleIconButton(
                        iconRes = R.drawable.ic_arrow_back,
                        contentDescription = stringResource(R.string.back_button_cd),
                        onClick = handleBack,
                        enabled = !isLoading,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Spacer(Modifier.height(32.dp))
            if (isSignUp) {
                StepIndicator(current = step)
                Spacer(Modifier.height(24.dp))
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        // Short travel + fade (same feel as screen-to-screen navigation)
                        val direction = if (targetState > initialState) 1 else -1
                        val easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
                        (slideInHorizontally(tween(380, easing = easing)) { it / 6 * direction } +
                            fadeIn(tween(220, delayMillis = 60, easing = easing))) togetherWith
                            (slideOutHorizontally(tween(380, easing = easing)) { -it / 6 * direction } +
                                fadeOut(tween(120))) using
                            SizeTransform(clip = false)
                    },
                    label = "signUpStep",
                ) { target ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        when (target) {
                            SignUpStep.Name -> {
                                StepHeader(
                                    title = stringResource(R.string.sign_up_name_title),
                                    subtitle = stringResource(R.string.sign_up_name_subtitle),
                                )
                                PickleTextField(
                                    value = name,
                                    onValueChange = onNameChange,
                                    placeholder = stringResource(R.string.sign_up_name_placeholder),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Words,
                                        imeAction = ImeAction.Next,
                                    ),
                                    keyboardActions = KeyboardActions(onNext = { goNext() }),
                                    enabled = !isLoading,
                                )
                            }

                            SignUpStep.Avatar -> {
                                StepHeader(
                                    title = stringResource(R.string.sign_up_avatar_title),
                                    subtitle = stringResource(R.string.sign_up_avatar_subtitle),
                                )
                                AvatarGrid(
                                    seeds = avatarSeeds,
                                    selectedSeed = selectedSeed,
                                    onSelect = onSelectAvatar,
                                )
                                Spacer(Modifier.height(12.dp))
                                PickleButton(
                                    text = stringResource(R.string.sign_up_shuffle),
                                    onClick = onShuffleAvatars,
                                    style = PickleButtonStyle.Outline,
                                    height = 36.dp,
                                    modifier = Modifier.align(Alignment.End),
                                )
                            }

                            SignUpStep.Email -> {
                                StepHeader(
                                    title = stringResource(R.string.sign_up_email_title),
                                    subtitle = stringResource(R.string.sign_up_email_subtitle),
                                )
                                EmailField(
                                    value = email,
                                    onValueChange = onEmailChange,
                                    imeAction = ImeAction.Next,
                                    onImeAction = { goNext() },
                                    enabled = !isLoading,
                                )
                            }

                            SignUpStep.Password -> {
                                StepHeader(
                                    title = stringResource(R.string.sign_up_password_title),
                                    subtitle = stringResource(R.string.sign_up_password_subtitle),
                                )
                                PasswordField(
                                    value = password,
                                    onValueChange = onPasswordChange,
                                    onDone = { goNext() },
                                    enabled = !isLoading,
                                )
                            }
                        }
                    }
                }
            } else {
                PickleBrand()
                Spacer(Modifier.height(24.dp))
                StepHeader(
                    title = stringResource(R.string.log_in_title),
                    subtitle = stringResource(R.string.log_in_subtitle),
                )
                EmailField(
                    value = email,
                    onValueChange = onEmailChange,
                    imeAction = ImeAction.Next,
                    onImeAction = null,
                    enabled = !isLoading,
                )
                Spacer(Modifier.height(12.dp))
                PasswordField(
                    value = password,
                    onValueChange = onPasswordChange,
                    onDone = { submit() },
                    enabled = !isLoading,
                )
            }
            Spacer(Modifier.height(24.dp))
            PickleButton(
                text = when {
                    !isSignUp -> stringResource(R.string.log_in)
                    step == SignUpStep.Password -> stringResource(R.string.create_account)
                    else -> stringResource(R.string.continue_button)
                },
                onClick = if (isSignUp) goNext else submit,
                enabled = if (isSignUp) isStepValid else canSignIn,
                loading = isLoading,
                style = PickleButtonStyle.Teal,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick = onToggleMode,
                enabled = !isLoading,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = if (isSignUp) stringResource(R.string.switch_to_log_in)
                    else stringResource(R.string.switch_to_sign_up),
                    style = MaterialTheme.typography.labelLarge,
                    color = Pickle.colors.textAccent,
                )
            }
        }
    }
}

@Composable
private fun StepIndicator(current: SignUpStep) {
    val total = SignUpStep.entries.size
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SignUpStep.entries.forEach { s ->
                val color by animateColorAsState(
                    targetValue = if (s <= current) Pickle.colors.teal
                    else Pickle.colors.sunken,
                    label = "stepDot",
                )
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(if (s == current) 32.dp else 16.dp)
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.sign_up_step, current.ordinal + 1, total),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StepHeader(title: String, subtitle: String) {
    PickleHeadline(text = title)
    Spacer(Modifier.height(8.dp))
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = Pickle.colors.inkMuted,
    )
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun AvatarGrid(
    seeds: List<String>,
    selectedSeed: String?,
    onSelect: (String) -> Unit,
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        seeds.chunked(5).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEachIndexed { columnIndex, seed ->
                    val isSelected = seed == selectedSeed
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) Pickle.colors.mustard
                        else Pickle.colors.lineStrong,
                        label = "avatarBorder",
                    )
                    AsyncImage(
                        model = remember(seed) {
                            ImageRequest.Builder(context)
                                .data(DiceBear.avatarUrl(seed))
                                .decoderFactory(SvgDecoder.Factory())
                                .build()
                        },
                        contentDescription = stringResource(
                            R.string.sign_up_avatar_cd,
                            rowIndex * 5 + columnIndex + 1,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(Pickle.colors.surface)
                            .border(if (isSelected) 4.dp else PickleBorderWidth, borderColor, CircleShape)
                            .clickable { onSelect(seed) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmailField(
    value: String,
    onValueChange: (String) -> Unit,
    imeAction: ImeAction,
    onImeAction: (() -> Unit)?,
    enabled: Boolean,
) {
    PickleTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = stringResource(R.string.email),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = imeAction,
        ),
        keyboardActions = if (onImeAction != null) KeyboardActions(onNext = { onImeAction() })
        else KeyboardActions.Default,
        enabled = enabled,
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    enabled: Boolean,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    PickleTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = stringResource(R.string.password),
        visualTransformation = if (visible) VisualTransformation.None
        else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        enabled = enabled,
        trailing = {
            IconButton(
                onClick = { visible = !visible },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    painter = painterResource(if (visible) R.drawable.ic_eye_off else R.drawable.ic_eye),
                    contentDescription = stringResource(
                        if (visible) R.string.hide_password_cd else R.string.show_password_cd
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}
