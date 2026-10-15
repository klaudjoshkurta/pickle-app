package com.shkurta.pickle.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.shkurta.pickle.R
import com.shkurta.pickle.ui.components.LocalActionsEnabled
import com.shkurta.pickle.ui.theme.PickleTheme
import com.shkurta.pickle.ui.theme.RubikFontFamily

private val OnboardingCream = Color(0xFFFAF3E3)
private val OnboardingTeal = Color(0xFF22786A)
private val OnboardingInk = Color(0xFF242424)
private val OnboardingSurface = Color(0xFFFFFCF7)

@Composable
fun OnboardingScreen(
    onCreateAccount: () -> Unit,
    onTryIt: () -> Unit,
    onLogIn: () -> Unit,
) {
    BoxWithConstraints(
        Modifier.fillMaxSize().background(OnboardingCream).safeDrawingPadding(),
    ) {
        val viewportHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight)
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(44.dp))
            Image(
                painter = painterResource(R.drawable.onboarding_image),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.weight(1f).heightIn(min = 36.dp))
            Image(
                painter = painterResource(R.drawable.pickle_logo),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(width = 87.dp, height = 32.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.onboarding_tagline),
                fontFamily = RubikFontFamily,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                color = OnboardingInk,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.weight(1f).heightIn(min = 36.dp))
            OnboardingButton(
                text = stringResource(R.string.onboarding_create_poll),
                onClick = onCreateAccount,
                container = OnboardingTeal,
                content = Color.White,
            )
            Spacer(Modifier.height(12.dp))
            OnboardingButton(
                text = stringResource(R.string.try_it),
                onClick = onTryIt,
                container = OnboardingSurface,
                content = OnboardingInk,
            )
            Spacer(Modifier.height(32.dp))
            TextButton(
                onClick = onLogIn,
                enabled = LocalActionsEnabled.current,
                modifier = Modifier.padding(horizontal = 24.dp).heightIn(min = 48.dp),
            ) {
                Text(
                    text = buildAnnotatedString {
                        append(stringResource(R.string.already_have_an_account))
                        withStyle(SpanStyle(color = OnboardingTeal)) {
                            append(stringResource(R.string.log_in))
                        }
                    },
                    fontFamily = RubikFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnboardingInk,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun OnboardingButton(
    text: String,
    onClick: () -> Unit,
    container: Color,
    content: Color,
) {
    Button(
        onClick = onClick,
        enabled = LocalActionsEnabled.current,
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth().heightIn(min = 58.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
    ) {
        Text(
            text = text,
            fontFamily = RubikFontFamily,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 917)
@Preview(showBackground = true, widthDp = 320, heightDp = 568, fontScale = 1.5f)
@Preview(showBackground = true, widthDp = 412, heightDp = 917, locale = "el")
@Composable
private fun OnboardingPreview() {
    PickleTheme {
        OnboardingScreen(onCreateAccount = {}, onTryIt = {}, onLogIn = {})
    }
}
