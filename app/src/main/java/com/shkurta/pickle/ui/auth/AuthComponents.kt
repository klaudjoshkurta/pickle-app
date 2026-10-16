package com.shkurta.pickle.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.svg.SvgDecoder
import com.shkurta.pickle.R
import com.shkurta.pickle.ui.components.*
import com.shkurta.pickle.ui.theme.Pickle
import com.shkurta.pickle.ui.theme.PickleTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AuthLayout(onBack: () -> Unit, loading: Boolean, scrollKey: Any = Unit, content: @Composable ColumnScope.() -> Unit) {
    val scrollState = key(scrollKey) { rememberScrollState() }
    BackHandler(enabled = loading) { /* Keep a pending request attached to its screen. */ }
    Scaffold(modifier = Modifier.imePadding(), topBar = {
        TopAppBar(title = {}, navigationIcon = {
            IconButton(onClick = onBack, enabled = !loading,
                modifier = Modifier.padding(start = 8.dp).size(48.dp).background(Color.White, CircleShape)) {
                Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.back_button_cd),
                    tint = Color(0xFF242424))
            }
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 480.dp).fillMaxWidth().verticalScroll(scrollState)
                .padding(horizontal = 24.dp).padding(top = 16.dp, bottom = 32.dp)) {
                content()
            }
        }
    }
}

@Composable
internal fun AuthHeader(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.headlineMedium, color = Pickle.colors.ink,
        modifier = Modifier.semantics { heading() })
    Spacer(Modifier.height(8.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = Pickle.colors.inkMuted)
}

@Composable
internal fun AuthSwitch(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
        TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(text)
        }
    }
}

@Composable
internal fun RequestFeedback(status: AuthStatus, registration: Boolean) {
    if (status == AuthStatus.Error || status == AuthStatus.Success) {
        Text(stringResource(when {
            status == AuthStatus.Success && registration -> R.string.auth_success
            status == AuthStatus.Success -> R.string.auth_login_success
            registration -> R.string.auth_register_failed
            else -> R.string.auth_login_failed
        }), color = if (status == AuthStatus.Error) MaterialTheme.colorScheme.error else Pickle.colors.textAccent,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp).semantics { liveRegion = LiveRegionMode.Polite })
    }
}

@Composable
internal fun blurValidation(onBlur: () -> Unit): Modifier {
    var hadFocus by remember { mutableStateOf(false) }
    return Modifier.onFocusChanged {
        if (hadFocus && !it.hasFocus) onBlur()
        hadFocus = it.hasFocus
    }
}

@Composable
internal fun AuthEmail(value: String, onChange: (String) -> Unit, enabled: Boolean, error: Boolean, onBlur: () -> Unit) {
    val focus = LocalFocusManager.current
    PickleTextField(value, onChange, stringResource(R.string.auth_email_placeholder), label = stringResource(R.string.email),
        enabled = enabled, modifier = blurValidation(onBlur), isError = error,
        supportingText = if (error) stringResource(R.string.auth_email_invalid) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(androidx.compose.ui.focus.FocusDirection.Next) }))
}

@Composable
internal fun AuthPassword(
    value: String, onChange: (String) -> Unit, enabled: Boolean, error: Boolean,
    errorText: String, onBlur: () -> Unit, onDone: () -> Unit, helper: String? = null,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    PickleTextField(value, onChange, stringResource(R.string.password), label = stringResource(R.string.password),
        enabled = enabled, modifier = blurValidation(onBlur), isError = error,
        supportingText = if (error) errorText else helper,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }), trailing = {
            IconButton(onClick = { visible = !visible }, enabled = enabled, modifier = Modifier.size(48.dp)) {
                Icon(painterResource(if (visible) R.drawable.ic_eye_off else R.drawable.ic_eye),
                    stringResource(if (visible) R.string.hide_password_cd else R.string.show_password_cd))
            }
        })
}

@Composable
internal fun AvatarGrid(seeds: List<String>, selectedSeed: String?, enabled: Boolean, onSelect: (String) -> Unit) {
    val context = LocalContext.current
    Column(Modifier.semantics { selectableGroup() }, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Four columns keep targets above 48dp even on a 320dp phone.
        seeds.chunked(4).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEachIndexed { columnIndex, seed ->
                    val selected = seed == selectedSeed
                    Box(Modifier.weight(1f).aspectRatio(1f).clip(CircleShape)
                        .background(Pickle.colors.surface)
                        .border(if (selected) 3.dp else PickleBorderWidth,
                            if (selected) Pickle.colors.teal else Pickle.colors.lineSoft, CircleShape)
                        .selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = { onSelect(seed) })
                        .semantics { contentDescription = context.getString(R.string.sign_up_avatar_cd, rowIndex * 4 + columnIndex + 1) },
                        contentAlignment = Alignment.Center) {
                        AsyncImage(model = remember(seed) { ImageRequest.Builder(context).data(DiceBear.avatarUrl(seed))
                            .decoderFactory(SvgDecoder.Factory()).build() }, contentDescription = null, modifier = Modifier.fillMaxSize())
                        if (selected) {
                            Icon(painterResource(R.drawable.ic_check), contentDescription = null,
                                tint = Pickle.colors.ink,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
                                    .size(20.dp).background(Pickle.colors.mustard, CircleShape).padding(3.dp))
                        }
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 568, fontScale = 1.5f)
@Preview(showBackground = true, widthDp = 412, heightDp = 917, locale = "el")
@Composable
private fun LoginPreview() = PickleTheme {
    LoginContent(LoginUiState(), {}, {}, {}, {}, {}, {})
}
@Preview(showBackground = true, widthDp = 320, heightDp = 568, fontScale = 1.5f)
@Preview(showBackground = true, widthDp = 412, heightDp = 917, locale = "el")
@Composable
private fun RegisterPreview() = PickleTheme {
    RegisterContent(RegisterUiState(), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
}
