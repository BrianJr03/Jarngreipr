package jr.brian.home.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import android.content.ClipData
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import kotlinx.coroutines.launch
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.model.FAQItem
import jr.brian.home.ui.components.settings.ScreenHeader
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.subtleCardGradient
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun FAQScreen(
    onDismiss: () -> Unit = {}
) {
    BackHandler(onBack = onDismiss)

    val faqItems = listOf(
        FAQItem(
            question = R.string.faq_theme_sharing_question,
            answer = R.string.faq_theme_sharing_answer
        ),

        FAQItem(
            question = R.string.faq_wallpaper_sharing_question,
            answer = R.string.faq_wallpaper_sharing_answer
        ),

        FAQItem(
            question = R.string.faq_keyboard_question,
            answer = R.string.faq_keyboard_answer
        ),

        FAQItem(
            question = R.string.faq_app_close_question,
            answer = R.string.faq_app_close_answer
        ),
    )

    Scaffold(
        containerColor = OledBackgroundColor,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OledBackgroundColor)
                .padding(innerPadding)
                .systemBarsPadding()
        ) {
            Column {
                ScreenHeader(onBackClick = onDismiss)

                Text(
                    text = stringResource(R.string.faq_screen_title),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { WallpaperAutomationFAQCard() }

                    items(faqItems) { faqItem ->
                        FAQCard(
                            question = stringResource(faqItem.question),
                            answer = stringResource(faqItem.answer)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperAutomationFAQCard() {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    fun copyText(text: String) {
        scope.launch {
            clipboard.setClipEntry(
                ClipEntry(ClipData.newPlainText("", text))
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = subtleCardGradient(isFocused = false),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                brush = borderBrush(
                    isFocused = true,
                    colors = listOf(
                        ThemePrimaryColor.copy(alpha = 0.3f),
                        ThemeSecondaryColor.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.faq_wallpaper_automation_question),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ThemePrimaryColor,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            val answerText = stringResource(R.string.faq_wallpaper_automation_answer)
            Text(
                text = buildAnnotatedString {
                    val keyword = "Broadcast"
                    val idx = answerText.indexOf(keyword)
                    if (idx >= 0) {
                        append(answerText.substring(0, idx))
                        withStyle(SpanStyle(color = ThemePrimaryColor)) { append(keyword) }
                        append(answerText.substring(idx + keyword.length))
                    } else {
                        append(answerText)
                    }
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.85f),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            CopyableRow(
                label = "ES-DE wallpaper action",
                value = "jr.brian.SET_ESDE_WALLPAPER",
                onCopy = { copyText(it) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            CopyableRow(
                label = "Standard wallpaper action",
                value = "jr.brian.SET_STANDARD_WALLPAPER",
                onCopy = { copyText(it) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            CopyableRow(
                label = "Package",
                value = "jr.brian.home",
                onCopy = { copyText(it) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            CopyableRow(
                label = "Class",
                value = "jr.brian.home.WallpaperActionReceiver",
                onCopy = { copyText(it) }
            )
        }
    }
}

@Composable
private fun CopyableRow(
    label: String,
    value: String,
    onCopy: (String) -> Unit
) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.5f),
            lineHeight = 16.sp
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = value,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { onCopy(value) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy $label",
                    tint = ThemePrimaryColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun FAQCard(
    question: String,
    answer: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = subtleCardGradient(isFocused = false),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                brush = borderBrush(
                    isFocused = true,
                    colors = listOf(
                        ThemePrimaryColor.copy(alpha = 0.3f),
                        ThemeSecondaryColor.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = question,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = ThemePrimaryColor,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = answer,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.85f),
                lineHeight = 22.sp
            )
        }
    }
}
