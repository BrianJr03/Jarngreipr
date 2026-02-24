package jr.brian.home.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import jr.brian.home.R
import jr.brian.home.ui.components.dialog.DimmedDialog
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor

@Composable
fun WhatsNewDialog(
    versionName: String,
    patchNotes: String,
    onDismiss: () -> Unit
) {
    DimmedDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f)
                .border(
                    width = 2.dp,
                    color = ThemePrimaryColor.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            color = OledCardColor,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.whats_new_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.whats_new_version, versionName),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.whats_new_close),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        MarkdownText(
                            text = patchNotes,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ThemePrimaryColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.whats_new_got_it))
                }
            }
        }
    }
}

@Composable
private fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        val lines = text.split("\n")
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            when {
                line.startsWith("### ") -> MarkdownHeader3(line)
                line.startsWith("## ") -> MarkdownHeader2(line)
                line.startsWith("# ") -> MarkdownHeader1(line)
                line.trim().startsWith("- ") || line.trim().startsWith("* ") ->
                    MarkdownBulletPoint(line)

                line.trim().startsWith(">") -> MarkdownBlockquote(line)
                line.trim().startsWith("```") -> {
                    i++
                    val codeLines = mutableListOf<String>()
                    while (i < lines.size && !lines[i].trim().startsWith("```")) {
                        codeLines.add(lines[i])
                        i++
                    }
                    MarkdownCodeBlock(codeLines)
                }

                line.contains("**") -> MarkdownBoldText(line)
                line.isBlank() -> MarkdownEmptyLine()
                else -> MarkdownRegularText(line)
            }
            i++
        }
    }
}

@Composable
private fun MarkdownHeader1(line: String) {
    Text(
        text = line.removePrefix("# "),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.padding(vertical = 16.dp)
    )
}

@Composable
private fun MarkdownHeader2(line: String) {
    Text(
        text = line.removePrefix("## "),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Composable
private fun MarkdownHeader3(line: String) {
    Text(
        text = line.removePrefix("### "),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun MarkdownBulletPoint(line: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Text(
            text = "• ",
            style = MaterialTheme.typography.bodyLarge,
            color = ThemePrimaryColor
        )
        Text(
            text = line.trim().removePrefix("- ").removePrefix("* "),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.85f),
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun MarkdownBlockquote(line: String) {
    val quoteText = line.trim().removePrefix(">").trimStart()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Surface(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp),
            color = ThemePrimaryColor.copy(alpha = 0.6f),
            shape = MaterialTheme.shapes.extraSmall
        ) {}
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = quoteText,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.6f),
            lineHeight = 24.sp,
            fontStyle = FontStyle.Italic
        )
    }
}

@Composable
private fun MarkdownCodeBlock(codeLines: List<String>) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        color = Color.White.copy(alpha = 0.08f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = codeLines.joinToString("\n"),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun MarkdownBoldText(line: String) {
    Text(
        text = line.replace("**", ""),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = if (line.trim().startsWith("**")) FontWeight.Bold else FontWeight.Normal,
        color = Color.White,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun MarkdownEmptyLine() {
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun MarkdownRegularText(line: String) {
    Text(
        text = line,
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White.copy(alpha = 0.85f),
        lineHeight = 24.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
