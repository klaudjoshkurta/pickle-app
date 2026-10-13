package com.shkurta.pickle.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shkurta.pickle.ui.theme.PickleTheme

@Composable
fun <T> SegmentedToggle(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = options.indexOf(selected).coerceAtLeast(0)

    Row(
        modifier = modifier
            .height(48.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex
            val pillColor by animateColorAsState(
                targetValue = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = if (isSelected) 1f else 0f,
                ),
                animationSpec = tween(250),
                label = "pill",
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
                },
                animationSpec = tween(250),
                label = "label",
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(pillColor)
                    .selectable(
                        selected = isSelected,
                        role = Role.Tab,
                        onClick = { onSelect(option) },
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAF2E3)
@Composable
private fun SegmentedTogglePreview() {
    PickleTheme {
        val options = listOf("My polls", "Closed")
        var selected by remember { mutableStateOf(options.first()) }

        SegmentedToggle(
            options = options,
            selected = selected,
            onSelect = { selected = it },
            label = { it },
            modifier = Modifier.padding(16.dp),
        )
    }
}
