package com.darmix.darmixpet.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

private val HIGHLIGHT_PATTERN = Regex("«([^»]+)»")

@Composable
fun highlighted(text: String, color: Color = MaterialTheme.colorScheme.primary): AnnotatedString =
    remember(text, color) {
        buildAnnotatedString {
            var cursor = 0
            HIGHLIGHT_PATTERN.findAll(text).forEach { match ->
                append(text.substring(cursor, match.range.first))
                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = color)) {
                    append(match.groupValues[1])
                }
                cursor = match.range.last + 1
            }
            append(text.substring(cursor))
        }
    }