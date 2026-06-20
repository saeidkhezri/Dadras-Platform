package com.example.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.InputStream
import java.util.zip.ZipInputStream
import java.util.regex.Pattern
import java.nio.charset.StandardCharsets

object DocumentProcessor {
    private const val TAG = "DocumentProcessor"

    fun extractTextFromUri(context: Context, uri: Uri, fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return "خطا در خواندن فایل"
            
            when (extension) {
                "txt", "md", "markdown" -> {
                    inputStream.use { stream ->
                        stream.bufferedReader(StandardCharsets.UTF_8).readText()
                    }
                }
                "docx" -> {
                    inputStream.use { stream ->
                        extractTextFromDocx(stream)
                    }
                }
                "pdf" -> {
                    inputStream.use { stream ->
                        extractTextFromPdf(stream)
                    }
                }
                "csv" -> {
                    inputStream.use { stream ->
                        parseCsv(stream)
                    }
                }
                "json" -> {
                    inputStream.use { stream ->
                        stream.bufferedReader(StandardCharsets.UTF_8).readText()
                    }
                }
                "xml" -> {
                    inputStream.use { stream ->
                        extractTextFromXml(stream)
                    }
                }
                "html", "htm" -> {
                    inputStream.use { stream ->
                        extractTextFromHtml(stream)
                    }
                }
                "rtf" -> {
                    inputStream.use { stream ->
                        extractTextFromRtf(stream)
                    }
                }
                else -> {
                    // Fallback to plain text stream
                    inputStream.use { stream ->
                        stream.bufferedReader(StandardCharsets.UTF_8).readText()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from $fileName", e)
            "خطا در پردازش فایل: ${e.localizedMessage}"
        }
    }

    private fun extractTextFromDocx(inputStream: InputStream): String {
        val zip = ZipInputStream(inputStream)
        var entry = zip.nextEntry
        val extracted = StringBuilder()
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                val content = zip.bufferedReader(StandardCharsets.UTF_8).readText()
                val matcher = Pattern.compile("<w:t[^>]*>(.*?)</w:t>").matcher(content)
                while (matcher.find()) {
                    val text = matcher.group(1) ?: continue
                    extracted.append(text).append(" ")
                }
                break
            }
            entry = zip.nextEntry
        }
        return extracted.toString().trim()
    }

    private fun extractTextFromPdf(inputStream: InputStream): String {
        val bytes = inputStream.readBytes()
        val str = String(bytes, StandardCharsets.ISO_8859_1) // ISO-8859-1 keeps raw byte values mapping clean
        val sb = StringBuilder()
        
        // Scan for standard text strings Tj / TJ in PDF page streams
        val matcher = Pattern.compile("\\((.*?)\\)\\s*(Tj|TJ)").matcher(str)
        while (matcher.find()) {
            val rawText = matcher.group(1) ?: continue
            // Decode basic PDF escape sequences
            val cleanStr = rawText
                .replace("\\(", "(")
                .replace("\\)", ")")
                .replace("\\\\", "\\")
            sb.append(cleanStr).append(" ")
        }
        
        var result = sb.toString().trim()
        if (result.length < 50) {
            // High durability Fallback: parse clean text characters directly from the file stream (e.g. for uncompressed plain PDFs)
            val cleanSB = StringBuilder()
            val charArray = str.toCharArray()
            for (c in charArray) {
                val code = c.code
                if (code in 32..126 || (code in 0x0600..0x06FF) || (code in 0xFB50..0xFEFF)) {
                    cleanSB.append(c)
                } else if (c == '\n' || c == '\r' || c == ' ') {
                    cleanSB.append(' ')
                }
            }
            result = cleanSB.toString().trim().replace("\\s+".toRegex(), " ")
        }
        return result
    }

    private fun parseCsv(inputStream: InputStream): String {
        val reader = inputStream.bufferedReader(StandardCharsets.UTF_8)
        val sb = StringBuilder()
        reader.forEachLine { line ->
            val cols = line.split(",")
            sb.append(cols.joinToString(" | ")).append("\n")
        }
        return sb.toString().trim()
    }

    private fun extractTextFromXml(inputStream: InputStream): String {
        val content = inputStream.bufferedReader(StandardCharsets.UTF_8).readText()
        // Strip out XML tags cleanly
        return content.replace("<[^>]*>".toRegex(), " ").replace("\\s+".toRegex(), " ").trim()
    }

    private fun extractTextFromHtml(inputStream: InputStream): String {
        val html = inputStream.bufferedReader(StandardCharsets.UTF_8).readText()
        // Strip script and style tags content
        var cleanHtml = html
            .replace("<script[^>]*?>.*?</script>".toRegex(RegexOption.IGNORE_CASE), " ")
            .replace("<style[^>]*?>.*?</style>".toRegex(RegexOption.IGNORE_CASE), " ")
        // Strip standard tags
        cleanHtml = cleanHtml.replace("<[^>]*>".toRegex(), " ")
        // Restore common entities
        cleanHtml = cleanHtml
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
        return cleanHtml.replace("\\s+".toRegex(), " ").trim()
    }

    private fun extractTextFromRtf(inputStream: InputStream): String {
        val content = inputStream.bufferedReader(StandardCharsets.UTF_8).readText()
        // Strip common RTF control words starting with backslash
        val cleanRtf = content.replace("\\\\[a-z0-9]+".toRegex(), " ").replace("[{}]".toRegex(), " ")
        return cleanRtf.replace("\\s+".toRegex(), " ").trim()
    }

    fun detectLanguage(text: String): String {
        if (text.isBlank()) return "نامشخص"
        var persianCharCount = 0
        var standardCharCount = 0
        
        for (char in text) {
            val code = char.code
            if (code in 0x0600..0x06FF || code in 0xFB50..0xFEFF || char == 'ی' || char == 'ک' || char == 'گ' || char == 'چ' || char == 'پ' || char == 'ژ') {
                persianCharCount++
            } else if (char.isLetter()) {
                standardCharCount++
            }
        }
        
        return when {
            persianCharCount > 0 && standardCharCount > 0 -> "فارسی / انگلیسی (مزدوج)"
            persianCharCount > 0 -> "فارسی (Persian)"
            standardCharCount > 0 -> "انگلیسی (English)"
            else -> "مزدوج"
        }
    }
}
