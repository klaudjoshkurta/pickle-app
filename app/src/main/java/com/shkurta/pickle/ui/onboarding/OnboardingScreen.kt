package com.shkurta.pickle.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.svg.SvgDecoder
import com.shkurta.pickle.R
import com.shkurta.pickle.ui.auth.DiceBear
import com.shkurta.pickle.ui.components.BlockMeter
import com.shkurta.pickle.ui.components.PickleBorderWidth
import com.shkurta.pickle.ui.components.PickleBrand
import com.shkurta.pickle.ui.components.PickleButton
import com.shkurta.pickle.ui.components.PickleButtonStyle
import com.shkurta.pickle.ui.components.PickleCardShape
import com.shkurta.pickle.ui.components.PickleHeadline
import com.shkurta.pickle.ui.theme.Pickle
import com.shkurta.pickle.ui.theme.PickleTheme

private val HeroCrewSeeds = listOf("Priya", "Jonah", "Lena", "Theo", "Omar", "Sam")

@Composable
fun OnboardingScreen(
    onCreateAccount: () -> Unit,
    onTryIt: () -> Unit,
    onLogIn: () -> Unit,
) {
    val c = Pickle.colors
    Scaffold(containerColor = c.canvas) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            PickleBrand()
            Spacer(Modifier.height(32.dp))
            HeroCard(modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(20.dp))
            CrewRow(modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(40.dp))
            PickleHeadline(
                text = stringResource(R.string.onboarding_headline),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.onboarding_description),
                style = MaterialTheme.typography.bodyMedium,
                color = c.inkMuted,
            )
            Spacer(Modifier.height(28.dp))
            PickleButton(
                text = stringResource(R.string.create_account),
                onClick = onCreateAccount,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            PickleButton(
                text = stringResource(R.string.try_it),
                onClick = onTryIt,
                modifier = Modifier.fillMaxWidth(),
                style = PickleButtonStyle.Outline,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.onboarding_guest_note),
                style = MaterialTheme.typography.labelSmall,
                color = c.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                onClick = onLogIn,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = buildAnnotatedString {
                        append(stringResource(R.string.already_have_an_account))
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = c.textAccent)) {
                            append(stringResource(R.string.log_in))
                        }
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = c.ink,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Tilted tomato "in the lead" card from the welcome illustration. */
@Composable
private fun HeroCard(modifier: Modifier = Modifier) {
    val c = Pickle.colors
    Column(
        modifier = modifier
            .rotate(-6f)
            .width(260.dp)
            .clip(PickleCardShape)
            .background(c.tomato)
            .border(PickleBorderWidth, c.lineStrong, PickleCardShape)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.onboarding_hero_label),
            style = MaterialTheme.typography.labelMedium,
            color = c.onBrand,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.onboarding_hero_option),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = c.onBrand,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "5",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = c.onBrand,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.onboarding_hero_votes),
                style = MaterialTheme.typography.labelMedium,
                color = c.onBrand,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        BlockMeter(filled = 5, total = 11, color = c.mustard, trackColor = c.tomatoDeep)
    }
}

@Composable
private fun CrewRow(modifier: Modifier = Modifier) {
    val c = Pickle.colors
    val context = LocalContext.current
    Box(modifier = modifier) {
        Row {
            HeroCrewSeeds.forEachIndexed { index, seed ->
                AsyncImage(
                    model = remember(seed) {
                        ImageRequest.Builder(context)
                            .data(DiceBear.avatarUrl(seed))
                            .decoderFactory(SvgDecoder.Factory())
                            .build()
                    },
                    contentDescription = null,
                    modifier = Modifier
                        .offset(x = (-8 * index).dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(c.surface)
                        .border(PickleBorderWidth, c.lineStrong, CircleShape),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF6EFE3)
@Composable
private fun OnboardingPreview() {
    PickleTheme {
        OnboardingScreen(
            onCreateAccount = {},
            onTryIt = {},
            onLogIn = {},
        )
    }
}
