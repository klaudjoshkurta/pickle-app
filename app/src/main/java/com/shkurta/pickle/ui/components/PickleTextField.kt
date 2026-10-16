package com.shkurta.pickle.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.shkurta.pickle.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shkurta.pickle.ui.theme.Pickle
import com.shkurta.pickle.ui.theme.PickleTheme

@Composable
fun PickleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
    label: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val colors = MaterialTheme.colorScheme
    val c = Pickle.colors

    Column(modifier = Modifier.fillMaxWidth()) {
        if (label != null) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = colors.onSurface)
            Spacer(Modifier.height(8.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth().semantics {
                if (label != null) contentDescription = label
                if (isError) error(supportingText.orEmpty())
            },
            enabled = enabled,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .background(c.surface, shape)
                        .border(
                            width = if (isFocused) 2.dp else PickleBorderWidth,
                            color = if (isError) colors.error else if (isFocused) c.teal else MaterialTheme.colorScheme.surface,
                            shape = shape,
                        )
                        .padding(horizontal = 16.dp)
                        .alpha(if (enabled) 1f else 0.6f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    }
                    if (trailing != null) {
                        Spacer(Modifier.width(8.dp))
                        trailing()
                    }
                }
            },
        )
        if (supportingText != null) {
            Spacer(Modifier.height(6.dp))
            Text(supportingText, style = MaterialTheme.typography.bodySmall,
                color = if (isError) colors.error else colors.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6EFE3)
@Composable
private fun PickleTextFieldPreview() {
    PickleTheme {
        var text by remember { mutableStateOf("") }

        Column(modifier = Modifier.padding(16.dp)) {
            PickleTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = stringResource(R.string.auth_name_label),
            )
            Spacer(Modifier.height(12.dp))
            PickleTextField(
                value = "hello@pickle.app",
                onValueChange = {},
                placeholder = stringResource(R.string.email),
            )
        }
    }
}
