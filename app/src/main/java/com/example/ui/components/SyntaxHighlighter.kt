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
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = highlightCodeSyntax(text.text, fileExtension)
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}

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
    // Batasi pewarnaan regex kompleks pada berkas sangat besar demi kelancaran 60fps
    if (source.length > 40_000) return AnnotatedString(source)

    val ext = extension.lowercase()
    return buildAnnotatedString {
        append(source)

        // 1. Komentar baris // atau # atau --
        val commentPattern = when (ext) {
            "py", "sh", "yaml", "yml", "toml", "rb" -> Regex("#[^\\n]*")
            "sql" -> Regex("--[^\\n]*")
            else -> Regex("//[^\\n]*|/\\*[\\s\\S]*?\\*/")
        }

        // 2. String literal "..." dan '...'
        val stringPattern = Regex("\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'")

        // 3. Anotasi @Something
        val annotationPattern = Regex("@[A-Za-z_][A-Za-z0-9_]*")

        // 4. Angka
        val numberPattern = Regex("\\b\\d+(\\.\\d+)?[fFlLuU]?\\b")

        // 5. Kata kunci & Tipe PascalCase
        val wordPattern = Regex("\\b[A-Za-z_][A-Za-z0-9_]*\\b")

        wordPattern.findAll(source).forEach { match ->
            val token = match.value
            when {
                token in COMMON_KEYWORDS -> addStyle(
                    SpanStyle(color = SyntaxKeyword),
                    match.range.first,
                    match.range.last + 1
                )
                token.first().isUpperCase() && token.any { it.isLowerCase() } -> addStyle(
                    SpanStyle(color = SyntaxType),
                    match.range.first,
                    match.range.last + 1
                )
            }
        }

        numberPattern.findAll(source).forEach { match ->
            addStyle(SpanStyle(color = SyntaxNumber), match.range.first, match.range.last + 1)
        }

        annotationPattern.findAll(source).forEach { match ->
            addStyle(SpanStyle(color = SyntaxAnnotation), match.range.first, match.range.last + 1)
        }

        stringPattern.findAll(source).forEach { match ->
            addStyle(SpanStyle(color = SyntaxString), match.range.first, match.range.last + 1)
        }

        commentPattern.findAll(source).forEach { match ->
            addStyle(
                SpanStyle(color = SyntaxComment, fontStyle = FontStyle.Italic),
                match.range.first,
                match.range.last + 1
            )
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
