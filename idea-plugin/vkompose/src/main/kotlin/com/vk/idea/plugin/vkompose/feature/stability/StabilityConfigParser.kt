package com.vk.idea.plugin.vkompose.feature.stability

import java.io.File

interface StabilityConfigParser {
    val stableTypeMatchers: Set<FqNameMatcher>

    companion object {
        fun fromFile(filepath: String?): StabilityConfigParser {
            if (filepath == null) return StabilityConfigParserImpl(emptyList())

            val confFile = File(filepath)
            if (!confFile.exists()) return StabilityConfigParserImpl(emptyList())

            return StabilityConfigParserImpl(confFile.readLines())
        }

        fun fromLines(lines: List<String>): StabilityConfigParser {
            return StabilityConfigParserImpl(lines)
        }
    }
}

private const val COMMENT_DELIMITER = "//"

private class StabilityConfigParserImpl(
    lines: List<String>
) : StabilityConfigParser {
    override val stableTypeMatchers: Set<FqNameMatcher>

    init {
        val matchers: MutableSet<FqNameMatcher> = mutableSetOf()

        lines.forEachIndexed { index, line ->
            val l = line.trim()
            if (!l.startsWith(COMMENT_DELIMITER) && !l.isBlank()) {
                if (l.contains(COMMENT_DELIMITER)) { // com.foo.bar //comment
                    error(
                        errorMessage(
                            line,
                            index,
                            "Comments are only supported at the start of a line."
                        )
                    )
                }
                try {
                    matchers.add(FqNameMatcher(l))
                } catch (exception: IllegalStateException) {
                    error(
                        errorMessage(line, index, exception.message ?: "")
                    )
                }
            }
        }

        stableTypeMatchers = matchers.toSet()
    }

    fun errorMessage(line: String, lineNumber: Int, message: String): String {
        return """
            Error parsing stability configuration file on line $lineNumber.
            $message
            $line
        """.trimIndent()
    }
}