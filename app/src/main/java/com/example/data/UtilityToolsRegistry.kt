package com.example.data

import java.math.BigInteger
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

enum class ToolCategory(val title: String, val icon: String) {
    MATH("Mathematics", "📐"),
    PHYSICS("Physics", "⚡"),
    CHEMISTRY("Chemistry", "⚗️"),
    BIOLOGY("Biology", "🧬"),
    LANGUAGE("English & Verbal", "📖"),
    APTITUDE("Logic & Aptitude", "🧩"),
    UNIT_CONVERTER("Unit Converters", "🔄"),
    TEXT("Text & Writing", "📝"),
    CRYPTO("Crypto & Numbers", "🔐"),
    DATA("Data & Web Tools", "💻"),
    TIME("Time & Date", "⏰"),
    DEVICE("Device & System", "📱")
}

data class AppUtilityTool(
    val id: String,
    val name: String,
    val category: ToolCategory,
    val description: String,
    val inputHint: String,
    val defaultInput: String,
    val execute: (String) -> String
)

object UtilityToolsRegistry {

    val allTools: List<AppUtilityTool> = listOf(
        // === UNIT CONVERTERS ===
        AppUtilityTool(
            id = "unit_length",
            name = "Length Converter",
            category = ToolCategory.UNIT_CONVERTER,
            description = "Convert meters into kilometers, centimeters, miles, yards, feet, and inches.",
            inputHint = "Value in meters (e.g. 1500)",
            defaultInput = "1000",
            execute = { input ->
                val m = input.toDoubleOrNull() ?: return@AppUtilityTool "Invalid numeric input"
                buildString {
                    appendLine("Meters (m): $m")
                    appendLine("Kilometers (km): ${m / 1000.0}")
                    appendLine("Centimeters (cm): ${m * 100.0}")
                    appendLine("Millimeters (mm): ${m * 1000.0}")
                    appendLine("Miles (mi): ${String.format(Locale.US, "%.5f", m * 0.000621371)}")
                    appendLine("Yards (yd): ${String.format(Locale.US, "%.3f", m * 1.09361)}")
                    appendLine("Feet (ft): ${String.format(Locale.US, "%.3f", m * 3.28084)}")
                    appendLine("Inches (in): ${String.format(Locale.US, "%.2f", m * 39.3701)}")
                }
            }
        ),
        AppUtilityTool(
            id = "unit_temp",
            name = "Temperature Converter",
            category = ToolCategory.UNIT_CONVERTER,
            description = "Convert Celsius to Fahrenheit and Kelvin simultaneously.",
            inputHint = "Temperature in °C (e.g. 25)",
            defaultInput = "25",
            execute = { input ->
                val c = input.toDoubleOrNull() ?: return@AppUtilityTool "Invalid temperature"
                val f = (c * 9.0 / 5.0) + 32.0
                val k = c + 273.15
                buildString {
                    appendLine("Celsius: $c °C")
                    appendLine("Fahrenheit: ${String.format(Locale.US, "%.2f", f)} °F")
                    appendLine("Kelvin: ${String.format(Locale.US, "%.2f", k)} K")
                }
            }
        ),
        AppUtilityTool(
            id = "unit_mass",
            name = "Mass & Weight Converter",
            category = ToolCategory.UNIT_CONVERTER,
            description = "Convert kilograms to grams, milligrams, pounds (lb), and ounces (oz).",
            inputHint = "Value in kg (e.g. 70)",
            defaultInput = "70",
            execute = { input ->
                val kg = input.toDoubleOrNull() ?: return@AppUtilityTool "Invalid weight"
                buildString {
                    appendLine("Kilograms (kg): $kg")
                    appendLine("Grams (g): ${kg * 1000.0}")
                    appendLine("Milligrams (mg): ${kg * 1000000.0}")
                    appendLine("Pounds (lbs): ${String.format(Locale.US, "%.3f", kg * 2.20462)}")
                    appendLine("Ounces (oz): ${String.format(Locale.US, "%.2f", kg * 35.274)}")
                }
            }
        ),
        AppUtilityTool(
            id = "unit_bytes",
            name = "Digital Storage Bytes Converter",
            category = ToolCategory.UNIT_CONVERTER,
            description = "Convert Megabytes (MB) to Bytes, Kilobytes, Gigabytes, and Terabytes.",
            inputHint = "Value in Megabytes (MB) (e.g. 1024)",
            defaultInput = "1024",
            execute = { input ->
                val mb = input.toDoubleOrNull() ?: return@AppUtilityTool "Invalid size"
                val bytes = mb * 1024.0 * 1024.0
                buildString {
                    appendLine("Bytes: ${bytes.toLong()}")
                    appendLine("Kilobytes (KB): ${mb * 1024.0}")
                    appendLine("Megabytes (MB): $mb")
                    appendLine("Gigabytes (GB): ${mb / 1024.0}")
                    appendLine("Terabytes (TB): ${String.format(Locale.US, "%.6f", mb / (1024.0 * 1024.0))}")
                }
            }
        ),
        AppUtilityTool(
            id = "unit_speed",
            name = "Speed Converter",
            category = ToolCategory.UNIT_CONVERTER,
            description = "Convert km/h into m/s, mph, and knots.",
            inputHint = "Speed in km/h (e.g. 120)",
            defaultInput = "100",
            execute = { input ->
                val kmh = input.toDoubleOrNull() ?: return@AppUtilityTool "Invalid speed"
                val ms = kmh / 3.6
                val mph = kmh * 0.621371
                val knots = kmh * 0.539957
                buildString {
                    appendLine("Kilometers per hour (km/h): $kmh")
                    appendLine("Meters per second (m/s): ${String.format(Locale.US, "%.2f", ms)}")
                    appendLine("Miles per hour (mph): ${String.format(Locale.US, "%.2f", mph)}")
                    appendLine("Knots (kn): ${String.format(Locale.US, "%.2f", knots)}")
                }
            }
        ),

        // === TEXT & WRITING ===
        AppUtilityTool(
            id = "text_counter",
            name = "Word & Sentence Counter",
            category = ToolCategory.TEXT,
            description = "Count words, total characters, characters without spaces, sentences, and paragraphs.",
            inputHint = "Paste text to analyze",
            defaultInput = "EUEE Mastery is an all-in-one educational super-app built for Ethiopian students. It supports offline learning, mathematics calculators, and science tools.",
            execute = { input ->
                val words = input.split("\\s+".toRegex()).count { it.isNotBlank() }
                val chars = input.length
                val charsNoSpace = input.count { !it.isWhitespace() }
                val sentences = input.split("[.!?]+".toRegex()).count { it.isNotBlank() }
                val paragraphs = input.split("\n+".toRegex()).count { it.isNotBlank() }
                val readingTimeSec = (words / 200.0 * 60).toInt()
                buildString {
                    appendLine("Word count: $words")
                    appendLine("Character count: $chars")
                    appendLine("Characters (no whitespace): $charsNoSpace")
                    appendLine("Sentences: $sentences")
                    appendLine("Paragraphs: $paragraphs")
                    appendLine("Estimated reading time: ~$readingTimeSec seconds")
                }
            }
        ),
        AppUtilityTool(
            id = "text_case",
            name = "Letter Case Converter",
            category = ToolCategory.TEXT,
            description = "Convert text to UPPERCASE, lowercase, Title Case, camelCase, snake_case, and kebab-case.",
            inputHint = "Enter sentence to transform",
            defaultInput = "ethiopian university entrance examination",
            execute = { input ->
                val upper = input.uppercase()
                val lower = input.lowercase()
                val words = input.trim().split("\\s+".toRegex())
                val title = words.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                val snake = words.joinToString("_") { it.lowercase() }
                val kebab = words.joinToString("-") { it.lowercase() }
                val camel = words.mapIndexed { idx, w -> if (idx == 0) w.lowercase() else w.replaceFirstChar { c -> c.uppercase() } }.joinToString("")
                buildString {
                    appendLine("UPPERCASE:\n$upper\n")
                    appendLine("lowercase:\n$lower\n")
                    appendLine("Title Case:\n$title\n")
                    appendLine("camelCase:\n$camel\n")
                    appendLine("snake_case:\n$snake\n")
                    appendLine("kebab-case:\n$kebab")
                }
            }
        ),
        AppUtilityTool(
            id = "text_reverse",
            name = "Text Reverser",
            category = ToolCategory.TEXT,
            description = "Reverses the entire string and reverses word order.",
            inputHint = "Enter text to reverse",
            defaultInput = "Hello EUEE Student",
            execute = { input ->
                val revChars = input.reversed()
                val revWords = input.trim().split("\\s+".toRegex()).reversed().joinToString(" ")
                buildString {
                    appendLine("Reversed Characters:\n$revChars\n")
                    appendLine("Reversed Words:\n$revWords")
                }
            }
        ),
        AppUtilityTool(
            id = "text_rot13",
            name = "ROT13 / Caesar Cipher",
            category = ToolCategory.TEXT,
            description = "Apply ROT13 cipher to encode or decode text.",
            inputHint = "Enter text to rotate",
            defaultInput = "Study hard for EUEE 2026",
            execute = { input ->
                val rot = input.map { c ->
                    when (c) {
                        in 'a'..'z' -> ((c - 'a' + 13) % 26 + 'a'.code).toChar()
                        in 'A'..'Z' -> ((c - 'A' + 13) % 26 + 'A'.code).toChar()
                        else -> c
                    }
                }.joinToString("")
                "ROT13 Result:\n$rot"
            }
        ),
        AppUtilityTool(
            id = "text_duplicate_lines",
            name = "Duplicate Line Remover",
            category = ToolCategory.TEXT,
            description = "Remove all duplicate lines while preserving original line order.",
            inputHint = "Paste multi-line text",
            defaultInput = "Apple\nBanana\nOrange\nApple\nGrape\nBanana",
            execute = { input ->
                val lines = input.lines().filter { it.isNotBlank() }
                val unique = lines.distinct()
                buildString {
                    appendLine("Original lines: ${lines.size}")
                    appendLine("Unique lines: ${unique.size}")
                    appendLine("Duplicates removed: ${lines.size - unique.size}\n")
                    appendLine("--- Cleaned Lines ---")
                    unique.forEach { appendLine(it) }
                }
            }
        ),

        // === CRYPTO & NUMBER SYSTEMS ===
        AppUtilityTool(
            id = "crypto_base",
            name = "Number Base Converter (Dec/Bin/Hex/Oct)",
            category = ToolCategory.CRYPTO,
            description = "Convert decimal number into binary, hexadecimal, and octal representations.",
            inputHint = "Decimal integer (e.g. 255)",
            defaultInput = "255",
            execute = { input ->
                val n = input.trim().toLongOrNull() ?: return@AppUtilityTool "Invalid integer"
                buildString {
                    appendLine("Decimal: $n")
                    appendLine("Binary: ${n.toString(2)}")
                    appendLine("Hexadecimal: 0x${n.toString(16).uppercase()}")
                    appendLine("Octal: 0o${n.toString(8)}")
                }
            }
        ),
        AppUtilityTool(
            id = "crypto_hash",
            name = "Hash Calculator (MD5 & SHA-256)",
            category = ToolCategory.CRYPTO,
            description = "Generate cryptographic hashes of text using MD5, SHA-1, and SHA-256.",
            inputHint = "Enter text to hash",
            defaultInput = "EUEE-Mastery-Passcode",
            execute = { input ->
                fun hash(alg: String): String {
                    val digest = MessageDigest.getInstance(alg)
                    val bytes = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
                    return bytes.joinToString("") { "%02x".format(it) }
                }
                buildString {
                    appendLine("MD5:\n${hash("MD5")}\n")
                    appendLine("SHA-1:\n${hash("SHA-1")}\n")
                    appendLine("SHA-256:\n${hash("SHA-256")}")
                }
            }
        ),
        AppUtilityTool(
            id = "crypto_password",
            name = "Secure Password Generator",
            category = ToolCategory.CRYPTO,
            description = "Generate cryptographically secure random passwords with entropy estimate.",
            inputHint = "Password length (e.g. 16)",
            defaultInput = "16",
            execute = { input ->
                val len = (input.trim().toIntOrNull() ?: 16).coerceIn(8, 64)
                val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-="
                val random = java.security.SecureRandom()
                val pwd = (1..len).map { chars[random.nextInt(chars.length)] }.joinToString("")
                val entropyBits = (len * (ln(chars.length.toDouble()) / ln(2.0))).toInt()
                buildString {
                    appendLine("Generated Password ($len characters):")
                    appendLine(pwd)
                    appendLine("\nEstimated Entropy: ~$entropyBits bits")
                    appendLine("Strength: Extremely Strong")
                }
            }
        ),
        AppUtilityTool(
            id = "crypto_roman",
            name = "Roman Numeral Converter",
            category = ToolCategory.CRYPTO,
            description = "Convert positive integers (1 to 3999) into Roman numerals.",
            inputHint = "Integer (1-3999)",
            defaultInput = "2026",
            execute = { input ->
                var num = input.trim().toIntOrNull() ?: return@AppUtilityTool "Invalid integer"
                if (num !in 1..3999) return@AppUtilityTool "Number must be between 1 and 3999"
                val map = listOf(
                    1000 to "M", 900 to "CM", 500 to "D", 400 to "CD",
                    100 to "C", 90 to "XC", 50 to "L", 40 to "XL",
                    10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I"
                )
                val sb = StringBuilder()
                for ((v, s) in map) {
                    while (num >= v) {
                        sb.append(s)
                        num -= v
                    }
                }
                "Roman Numeral:\n$sb"
            }
        ),
        AppUtilityTool(
            id = "crypto_uuid",
            name = "UUID v4 Generator",
            category = ToolCategory.CRYPTO,
            description = "Generate random Universally Unique Identifiers (UUIDs).",
            inputHint = "Number of UUIDs (e.g. 3)",
            defaultInput = "3",
            execute = { input ->
                val count = (input.trim().toIntOrNull() ?: 3).coerceIn(1, 20)
                buildString {
                    appendLine("Generated UUIDs (v4):")
                    for (i in 1..count) {
                        appendLine("$i. ${UUID.randomUUID()}")
                    }
                }
            }
        ),

        // === DATA & WEB TOOLS ===
        AppUtilityTool(
            id = "data_base64",
            name = "Base64 Encoder & Decoder",
            category = ToolCategory.DATA,
            description = "Encode string to Base64 or decode from Base64.",
            inputHint = "Enter text to encode or Base64 to decode",
            defaultInput = "Hello EUEE Student!",
            execute = { input ->
                val encoded = android.util.Base64.encodeToString(input.toByteArray(StandardCharsets.UTF_8), android.util.Base64.NO_WRAP)
                val decoded = runCatching {
                    val bytes = android.util.Base64.decode(input.trim(), android.util.Base64.DEFAULT)
                    String(bytes, StandardCharsets.UTF_8)
                }.getOrNull()
                buildString {
                    appendLine("Encoded (to Base64):\n$encoded\n")
                    if (decoded != null && decoded.any { it.isLetterOrDigit() }) {
                        appendLine("Decoded (from input as Base64):\n$decoded")
                    }
                }
            }
        ),
        AppUtilityTool(
            id = "data_url",
            name = "URL Encoder / Decoder",
            category = ToolCategory.DATA,
            description = "Encode special characters for URLs or decode encoded URI components.",
            inputHint = "Enter URL query string or text",
            defaultInput = "https://example.com/search?query=chemistry & physics questions=1",
            execute = { input ->
                val encoded = URLEncoder.encode(input, "UTF-8")
                val decoded = runCatching { URLDecoder.decode(input, "UTF-8") }.getOrDefault("")
                buildString {
                    appendLine("URL Encoded:\n$encoded\n")
                    appendLine("URL Decoded:\n$decoded")
                }
            }
        ),

        // === TIME & DATE ===
        AppUtilityTool(
            id = "time_days_between",
            name = "Date Difference Calculator",
            category = ToolCategory.TIME,
            description = "Calculate the number of days, weeks, and hours between two dates (YYYY-MM-DD).",
            inputHint = "Start Date, End Date (e.g. 2026-01-01, 2026-06-30)",
            defaultInput = "2026-01-01, 2026-06-30",
            execute = { input ->
                val parts = input.split(",").map { it.trim() }
                if (parts.size != 2) return@AppUtilityTool "Format: YYYY-MM-DD, YYYY-MM-DD"
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                try {
                    val d1 = sdf.parse(parts[0])?.time ?: return@AppUtilityTool "Invalid start date"
                    val d2 = sdf.parse(parts[1])?.time ?: return@AppUtilityTool "Invalid end date"
                    val diffMs = abs(d2 - d1)
                    val days = diffMs / (1000 * 60 * 60 * 24)
                    val weeks = days / 7
                    val remDays = days % 7
                    buildString {
                        appendLine("Total Days: $days days")
                        appendLine("Weeks: $weeks weeks and $remDays days")
                        appendLine("Hours: ${days * 24} hours")
                    }
                } catch (e: Exception) {
                    "Error parsing dates. Please use YYYY-MM-DD format."
                }
            }
        ),
        AppUtilityTool(
            id = "time_unix",
            name = "Unix Timestamp Converter",
            category = ToolCategory.TIME,
            description = "Convert Unix epoch milliseconds into human-readable date and vice-versa.",
            inputHint = "Timestamp in ms or 'now'",
            defaultInput = "now",
            execute = { input ->
                val ts = if (input.trim().lowercase() == "now") System.currentTimeMillis()
                else input.trim().toLongOrNull() ?: System.currentTimeMillis()
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US)
                buildString {
                    appendLine("Unix Timestamp (ms): $ts")
                    appendLine("Unix Seconds: ${ts / 1000}")
                    appendLine("Formatted Date (UTC/Local): ${sdf.format(Date(ts))}")
                }
            }
        )
    )
}
