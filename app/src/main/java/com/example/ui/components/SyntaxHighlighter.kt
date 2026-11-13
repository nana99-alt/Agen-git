package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.ui.theme.SyntaxAnnotation
import com.example.ui.theme.SyntaxComment
import com.example.ui.theme.SyntaxKeyword
import com.example.ui.theme.SyntaxNumber
import com.example.ui.theme.SyntaxString
import com.example.ui.theme.SyntaxType

class CodeSyntaxVisualTransformation(
    private val fileExtension: String
) : VisualTransformation {
    private var cachedSource: String? = null
    private var cachedResult: AnnotatedString? = null

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val highlighted = if (raw == cachedSource && cachedResult != null) {
            cachedResult!!
        } else {
            highlightCodeSyntax(raw, fileExtension).also {
                cachedSource = raw
                cachedResult = it
            }
        }
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}

private val HASH_COMMENT_REGEX = Regex("#[^\\n]*")
private val SQL_COMMENT_REGEX = Regex("--[^\\n]*")
private val C_COMMENT_REGEX = Regex("//[^\\n]*|/\\*[\\s\\S]*?\\*/")
private val STRING_REGEX = Regex("\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'")
private val ANNOTATION_REGEX = Regex("@[A-Za-z_][A-Za-z0-9_]*")
private val NUMBER_REGEX = Regex("\\b\\d+(\\.\\d+)?[fFlLuU]?\\b")
private val WORD_REGEX = Regex("\\b[A-Za-z_][A-Za-z0-9_]*\\b")

private val KEYWORD_SPAN = SpanStyle(color = SyntaxKeyword)
private val TYPE_SPAN = SpanStyle(color = SyntaxType)
private val NUMBER_SPAN = SpanStyle(color = SyntaxNumber)
private val ANNOTATION_SPAN = SpanStyle(color = SyntaxAnnotation)
private val STRING_SPAN = SpanStyle(color = SyntaxString)
private val COMMENT_SPAN = SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic)

private val COMMON_KEYWORDS = setOf(
    "package", "import", "class", "interface", "object", "fun", "val", "var",
    "if", "else", "when", "for", "while", "do", "return", "break", "continue",
    "try", "catch", "finally", "throw", "true", "false", "null", "this", "super",
    "private", "public", "protected", "internal", "override", "open", "abstract",
    "sealed", "data", "enum", "suspend", "inline", "companion", "init", "lateinit",
    "def", "async", "await", "function", "const", "let", "export", "default",
    "from", "switch", "case", "new", "void", "static", "final", "extends",
    "implements", "struct", "fn", "impl", "pub", "use", "mod", "match", "mut",
    "in", "is", "as", "by", "with", "yield", "elif", "pass", "lambda", "self"
)

fun highlightCodeSyntax(source: String, extension: String): AnnotatedString {
    if (source.isEmpty()) return AnnotatedString("")
    if (source.length > 30_000) return AnnotatedString(source)

    val ext = extension.lowercase()
    return buildAnnotatedString {
        append(source)

        val commentPattern = when (ext) {
            "py", "sh", "yaml", "yml", "toml", "rb" -> HASH_COMMENT_REGEX
            "sql" -> SQL_COMMENT_REGEX
            else -> C_COMMENT_REGEX
        }

        WORD_REGEX.findAll(source).forEach { match ->
            val token = match.value
            if (token in COMMON_KEYWORDS) {
                addStyle(KEYWORD_SPAN, match.range.first, match.range.last + 1)
            } else if (token.first().isUpperCase() && token.any { it.isLowerCase() }) {
                addStyle(TYPE_SPAN, match.range.first, match.range.last + 1)
            }
        }

        NUMBER_REGEX.findAll(source).forEach { match ->
            addStyle(NUMBER_SPAN, match.range.first, match.range.last + 1)
        }

        ANNOTATION_REGEX.findAll(source).forEach { match ->
            addStyle(ANNOTATION_SPAN, match.range.first, match.range.last + 1)
        }

        STRING_REGEX.findAll(source).forEach { match ->
            addStyle(STRING_SPAN, match.range.first, match.range.last + 1)
        }

        commentPattern.findAll(source).forEach { match ->
            addStyle(COMMENT_SPAN, match.range.first, match.range.last + 1)
        }
    }
}

fun languageColor(language: String?): Color {
    return when (language?.lowercase()) {
        "kotlin" -> Color(0xFFA97BFF)
        "java" -> Color(0xFFB07219)
        "python" -> Color(0xFF3572A5)
        "javascript" -> Color(0xFFF1E05A)
        "typescript" -> Color(0xFF3178C6)
        "dart" -> Color(0xFF00B4AB)
        "go" -> Color(0xFF00ADD8)
        "rust" -> Color(0xFFDEA584)
        "swift" -> Color(0xFFF05138)
        "c", "c++" -> Color(0xFFF34B7D)
        "html", "xml" -> Color(0xFFE34C26)
        else -> Color(0xFF38BDF8)
    }
}
