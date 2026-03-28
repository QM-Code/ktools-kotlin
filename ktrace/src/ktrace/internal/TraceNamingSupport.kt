package ktrace.internal

import ktrace.TraceColors

object TraceNamingSupport {
    private val identifier = Regex("[A-Za-z0-9_-]+")
    private val knownColorNames = listOf(
        "Black",
        "Red",
        "Green",
        "Yellow",
        "Blue",
        "Magenta",
        "Cyan",
        "White",
        "BrightBlack",
        "BrightRed",
        "BrightGreen",
        "BrightYellow",
        "BrightBlue",
        "BrightMagenta",
        "BrightCyan",
        "BrightWhite",
        "DeepSkyBlue1",
        "Gold3",
        "MediumSpringGreen",
        "Orange3",
        "MediumOrchid1",
        "LightSkyBlue1",
        "LightSalmon1",
    )

    fun color(colorName: String): Int {
        val token = trimWhitespace(colorName)
        require(token.isNotEmpty()) { "trace color name must not be empty" }
        if (token == "Default" || token == "default") {
            return TraceColors.DEFAULT
        }
        val index = knownColorNames.indexOf(token)
        require(index >= 0) { "unknown trace color '$token'" }
        return index
    }

    fun colorNames(): List<String> = knownColorNames

    fun trimWhitespace(value: String?): String = value?.trim().orEmpty()

    fun normalizeNamespace(traceNamespace: String): String {
        val token = trimWhitespace(traceNamespace)
        require(isSelectorIdentifier(token)) { "invalid trace namespace '$token'" }
        return token
    }

    fun normalizeChannel(channel: String): String {
        val token = trimWhitespace(channel)
        require(isValidChannelPath(token)) { "invalid trace channel '$token'" }
        return token
    }

    fun makeQualifiedChannelKey(traceNamespace: String, channel: String): String {
        val namespaceName = trimWhitespace(traceNamespace)
        val channelName = trimWhitespace(channel)
        if (namespaceName.isEmpty() || channelName.isEmpty()) {
            return ""
        }
        return "$namespaceName.$channelName"
    }

    fun isSelectorIdentifier(token: String): Boolean {
        val value = trimWhitespace(token)
        return value.isNotEmpty() && identifier.matches(value)
    }

    fun isValidChannelPath(channel: String): Boolean {
        val parts = splitChannelPath(channel)
        if (parts.isEmpty() || parts.size > 3) {
            return false
        }
        return parts.all { isSelectorIdentifier(it) }
    }

    fun splitChannelPath(channel: String): List<String> {
        val token = trimWhitespace(channel)
        if (token.isEmpty()) {
            return emptyList()
        }
        return token.split('.')
    }
}
