package com.shkurta.pickle.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.runtime.collectAsState
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
import com.shkurta.pickle.ui.components.PickleTextField

private enum class SignUpStep { Name, Avatar, Email, Password }

private const val MIN_PASSWORD_LENGTH = 6

@Composable
fun AuthScreen(
    initialSignUp: Boolean = false,
    onBack: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var isSignUp by rememberSaveable { mutableStateOf(initialSignUp) }
    var step by rememberSaveable { mutableStateOf(SignUpStep.Name) }
    var name by rememberSaveable { mutableStateOf("") }
    var avatarSeeds by rememberSaveable { mutableStateOf(DiceBear.randomSeeds()) }
    var selectedSeed by rememberSaveable { mutableStateOf<String?>(null) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val isLoading = uiState is AuthUiState.Loading

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Error) {
            snackbarHostState.showSnackbar((uiState as AuthUiState.Error).message)
            viewModel.clearError()
        }
    }

    val isEmailValid = email.trim().contains('@')
    val isPasswordValid = password.length >= MIN_PASSWORD_LENGTH
    val isStepValid = when (step) {
        SignUpStep.Name -> name.isNotBlank()
        SignUpStep.Avatar -> selectedSeed != null
        SignUpStep.Email -> isEmailValid
        SignUpStep.Password -> isPasswordValid
    }
    val canSignIn = email.isNotBlank() && password.isNotBlank()

    val submit = {
        if (isSignUp) {
            val seed = selectedSeed
            if (name.isNotBlank() && seed != null && isEmailValid && isPasswordValid) {
                viewModel.signUp(email.trim(), password, name.trim(), DiceBear.avatarUrl(seed))
            }
        } else if (canSignIn) {
            viewModel.signIn(email.trim(), password)
        }
    }

    val goNext = {
        if (isStepValid) {
            if (step == SignUpStep.Password) submit()
            else step = SignUpStep.entries[step.ordinal + 1]
        }
    }

    val canGoBackStep = isSignUp && step != SignUpStep.Name
    val handleBack = {
        if (canGoBackStep) step = SignUpStep.entries[step.ordinal - 1]
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
                    FilledIconButton(
                        onClick = handleBack,
                        enabled = !isLoading,
                        colors = IconButtonDefaults.outlinedIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.back_button_cd),
                        )
                    }
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
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))
            if (isSignUp) {
                StepIndicator(current = step)
                Spacer(Modifier.height(24.dp))
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        (slideInHorizontally { it * direction } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it * direction } + fadeOut())
                    },
                    label = "signUpStep",
                ) { target ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        when (target) {
                            SignUpStep.Name -> {
                                StepHeader(
                                    title = stringResource(R.string.sign_up_name_title),
                                    subtitle = stringResource(R.string.sign_up_name_subtitle),
                                )
                                PickleTextField(
                                    value = name,
                                    onValueChange = { name = it },
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
                                    onSelect = { selectedSeed = it },
                                )
                                Spacer(Modifier.height(8.dp))
                                TextButton(
                                    onClick = {
                                        avatarSeeds = DiceBear.randomSeeds()
                                        selectedSeed = null
                                    },
                                ) {
                                    Text(stringResource(R.string.sign_up_shuffle))
                                }
                            }

                            SignUpStep.Email -> {
                                StepHeader(
                                    title = stringResource(R.string.sign_up_email_title),
                                    subtitle = stringResource(R.string.sign_up_email_subtitle),
                                )
                                EmailField(
                                    value = email,
                                    onValueChange = { email = it },
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
                                    onValueChange = { password = it },
                                    onDone = { goNext() },
                                    enabled = !isLoading,
                                )
                            }
                        }
                    }
                }
            } else {
                Icon(
                    painter = painterResource(R.drawable.pickle_logo),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                )
                Spacer(Modifier.height(24.dp))
                StepHeader(
                    title = stringResource(R.string.log_in_title),
                    subtitle = stringResource(R.string.log_in_subtitle),
                )
                EmailField(
                    value = email,
                    onValueChange = { email = it },
                    imeAction = ImeAction.Next,
                    onImeAction = null,
                    enabled = !isLoading,
                )
                Spacer(Modifier.height(12.dp))
                PasswordField(
                    value = password,
                    onValueChange = { password = it },
                    onDone = { submit() },
                    enabled = !isLoading,
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = if (isSignUp) goNext else submit,
                enabled = !isLoading && if (isSignUp) isStepValid else canSignIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = when {
                            !isSignUp -> stringResource(R.string.log_in)
                            step == SignUpStep.Password -> stringResource(R.string.create_account)
                            else -> stringResource(R.string.continue_button)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            TextButton(
                onClick = {
                    isSignUp = !isSignUp
                    step = SignUpStep.Name
                },
                enabled = !isLoading,
            ) {
                Text(
                    text = if (isSignUp) stringResource(R.string.switch_to_log_in)
                    else stringResource(R.string.switch_to_sign_up),
                )
            }
        }
    }
}

@Composable
private fun StepIndicator(current: SignUpStep) {
    val total = SignUpStep.entries.size
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SignUpStep.entries.forEach { s ->
                val color by animateColorAsState(
                    targetValue = if (s <= current) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    label = "stepDot",
                )
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(if (s == current) 24.dp else 12.dp)
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
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
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
        seeds.chunked(3).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEachIndexed { columnIndex, seed ->
                    val isSelected = seed == selectedSeed
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Transparent,
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
                            rowIndex * 3 + columnIndex + 1,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(3.dp, borderColor, CircleShape)
                            .clickable { onSelect(seed) }
                            .padding(8.dp),
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
