package ktrace.internal

object TraceSelectorSupport {
    fun resolveExactChannel(
        loggerData: LoggerData,
        qualifiedChannel: String,
        localNamespace: String,
    ): ExactChannelResolution {
        val qualified = TraceNamingSupport.trimWhitespace(qualifiedChannel)
        val dot = qualified.indexOf('.')
        require(dot >= 0) {
            "invalid channel selector '$qualified' (expected namespace.channel or .channel; use .channel for local namespace)"
        }

        val traceNamespace = if (dot == 0) TraceNamingSupport.trimWhitespace(localNamespace) else qualified.substring(0, dot)
        val channel = qualified.substring(dot + 1)
        require(TraceNamingSupport.isSelectorIdentifier(traceNamespace)) { "invalid trace namespace '$traceNamespace'" }
        require(TraceNamingSupport.isValidChannelPath(channel)) { "invalid trace channel '$channel'" }
        val key = TraceNamingSupport.makeQualifiedChannelKey(traceNamespace, channel)
        return ExactChannelResolution(
            key,
            traceNamespace,
            channel,
            TraceRegistrySupport.isRegisteredTraceChannel(loggerData, traceNamespace, channel),
        )
    }

    fun resolveSelectorExpression(
        loggerData: LoggerData,
        selectorsCsv: String,
        localNamespace: String,
    ): SelectorResolution {
        val selectorText = TraceNamingSupport.trimWhitespace(selectorsCsv)
        require(selectorText.isNotEmpty()) { "EnableChannels requires one or more selectors" }

        val invalidTokens = mutableListOf<String>()
        val selectors = parseSelectorList(selectorText, localNamespace, invalidTokens)
        if (invalidTokens.isNotEmpty()) {
            val builder = StringBuilder()
            builder.append("Invalid trace selector")
            if (invalidTokens.size > 1) {
                builder.append('s')
            }
            builder.append(": ")
            invalidTokens.forEachIndexed { index, token ->
                if (index > 0) {
                    builder.append(", ")
                }
                builder.append(formatInvalidSelector(token))
            }
            throw IllegalArgumentException(builder.toString())
        }
        return resolveSelectorsToChannelKeys(loggerData, selectors)
    }

    private fun parseSelectorList(
        value: String,
        localNamespace: String,
        invalidTokens: MutableList<String>,
    ): List<Selector> {
        val selectors = mutableListOf<Selector>()
        val selectorTokens = mutableListOf<String>()
        val splitError = splitByTopLevelCommas(value, selectorTokens)
        if (splitError != null) {
            invalidTokens.add(splitError)
            return selectors
        }

        val invalidSeen = linkedSetOf<String>()
        for (token in selectorTokens) {
            val name = TraceNamingSupport.trimWhitespace(token)
            if (name.isEmpty()) {
                if (invalidSeen.add("<empty>")) {
                    invalidTokens.add("<empty>")
                }
                continue
            }

            val expandedTokens = mutableListOf<String>()
            val expandError = expandBraceExpression(name, expandedTokens)
            if (expandError != null) {
                val reason = "$name ($expandError)"
                if (invalidSeen.add(reason)) {
                    invalidTokens.add(reason)
                }
                continue
            }

            for (expanded in expandedTokens) {
                try {
                    selectors.add(parseSelector(expanded, localNamespace))
                } catch (ex: IllegalArgumentException) {
                    val reason = "$expanded (${ex.message})"
                    if (invalidSeen.add(reason)) {
                        invalidTokens.add(reason)
                    }
                }
            }
        }
        return selectors
    }

    private fun resolveSelectorsToChannelKeys(
        loggerData: LoggerData,
        selectors: List<Selector>,
    ): SelectorResolution {
        val channelKeys = mutableListOf<String>()
        val unmatchedSelectors = mutableListOf<String>()
        val seenKeys = linkedSetOf<String>()
        val matched = BooleanArray(selectors.size)

        synchronized(loggerData.registryLock) {
            for ((traceNamespace, channels) in loggerData.channelsByNamespace) {
                for (channel in channels) {
                    for (index in selectors.indices) {
                        if (!matchesSelector(selectors[index], traceNamespace, channel)) {
                            continue
                        }
                        matched[index] = true
                        val key = TraceNamingSupport.makeQualifiedChannelKey(traceNamespace, channel)
                        if (seenKeys.add(key)) {
                            channelKeys.add(key)
                        }
                    }
                }
            }
        }

        val unmatchedSeen = linkedSetOf<String>()
        for (index in selectors.indices) {
            if (matched[index]) {
                continue
            }
            val selectorText = formatSelector(selectors[index])
            if (unmatchedSeen.add(selectorText)) {
                unmatchedSelectors.add(selectorText)
            }
        }

        return SelectorResolution(channelKeys, unmatchedSelectors)
    }

    private fun parseSelector(rawToken: String, localNamespace: String): Selector {
        val dot = rawToken.indexOf('.')
        require(dot >= 0) { "did you mean '.*'?" }

        val namespaceToken = rawToken.substring(0, dot)
        val channelPattern = rawToken.substring(dot + 1)
        var anyNamespace = false
        var traceNamespace = ""
        when {
            namespaceToken == "*" -> anyNamespace = true
            namespaceToken.isEmpty() -> {
                traceNamespace = TraceNamingSupport.trimWhitespace(localNamespace)
                require(TraceNamingSupport.isSelectorIdentifier(traceNamespace)) { "missing namespace" }
            }
            TraceNamingSupport.isSelectorIdentifier(namespaceToken) -> traceNamespace = namespaceToken
            else -> throw IllegalArgumentException("invalid namespace '$namespaceToken'")
        }

        val tokens = TraceNamingSupport.splitChannelPath(channelPattern)
        require(tokens.isNotEmpty()) { "missing channel expression" }
        require(tokens.size <= 3) { "channel depth exceeds 3" }
        for (token in tokens) {
            require(token == "*" || TraceNamingSupport.isSelectorIdentifier(token)) { "invalid channel token '$token'" }
        }
        val includeTopLevel = tokens.size == 2 && tokens[0] == "*" && tokens[1] == "*"
        return Selector(anyNamespace, traceNamespace, tokens.toList(), includeTopLevel)
    }

    private fun matchesSelector(selector: Selector, traceNamespace: String, channel: String): Boolean {
        if (!selector.anyNamespace && traceNamespace != selector.traceNamespace) {
            return false
        }
        val channelParts = TraceNamingSupport.splitChannelPath(channel)
        if (channelParts.isEmpty()) {
            return false
        }
        val pattern = selector.channelTokens
        return when (pattern.size) {
            1 -> channelParts.size == 1 && matchesSelectorSegment(pattern[0], channelParts[0])
            2 -> {
                if (channelParts.size == 1 && selector.includeTopLevel) {
                    true
                } else {
                    channelParts.size == 2 &&
                        matchesSelectorSegment(pattern[0], channelParts[0]) &&
                        matchesSelectorSegment(pattern[1], channelParts[1])
                }
            }
            3 -> {
                if (pattern[0] == "*" && pattern[1] == "*" && pattern[2] == "*") {
                    channelParts.size in 1..3
                } else {
                    channelParts.size == 3 &&
                        matchesSelectorSegment(pattern[0], channelParts[0]) &&
                        matchesSelectorSegment(pattern[1], channelParts[1]) &&
                        matchesSelectorSegment(pattern[2], channelParts[2])
                }
            }
            else -> false
        }
    }

    private fun matchesSelectorSegment(pattern: String, value: String): Boolean = pattern == "*" || pattern == value

    private fun splitByTopLevelCommas(value: String, parts: MutableList<String>): String? {
        var braceDepth = 0
        var start = 0
        for (index in value.indices) {
            when (value[index]) {
                '{' -> braceDepth += 1
                '}' -> {
                    if (braceDepth == 0) {
                        return "unmatched '}'"
                    }
                    braceDepth -= 1
                }
                ',' -> if (braceDepth == 0) {
                    parts.add(TraceNamingSupport.trimWhitespace(value.substring(start, index)))
                    start = index + 1
                }
            }
        }
        if (braceDepth != 0) {
            return "unmatched '{'"
        }
        parts.add(TraceNamingSupport.trimWhitespace(value.substring(start)))
        return null
    }

    private fun expandBraceExpression(value: String, expanded: MutableList<String>): String? {
        val open = value.indexOf('{')
        if (open < 0) {
            expanded.add(value)
            return null
        }

        var depth = 0
        var close = -1
        for (index in open until value.length) {
            when (value[index]) {
                '{' -> depth += 1
                '}' -> {
                    depth -= 1
                    if (depth == 0) {
                        close = index
                        break
                    }
                }
            }
        }
        if (close < 0) {
            return "unmatched '{'"
        }

        val prefix = value.substring(0, open)
        val suffix = value.substring(close + 1)
        val inside = value.substring(open + 1, close)
        val alternatives = mutableListOf<String>()
        val splitError = splitByTopLevelCommas(inside, alternatives)
        if (splitError != null) {
            return splitError
        }
        if (alternatives.isEmpty()) {
            return "empty brace group"
        }
        for (alternative in alternatives) {
            if (alternative.isEmpty()) {
                return "empty brace alternative"
            }
            val nestedError = expandBraceExpression(prefix + alternative + suffix, expanded)
            if (nestedError != null) {
                return nestedError
            }
        }
        return null
    }

    private fun formatInvalidSelector(token: String): String {
        val reasonPos = token.indexOf(" (")
        return if (reasonPos >= 0) {
            "'${token.substring(0, reasonPos)}'${token.substring(reasonPos)}"
        } else {
            "'$token'"
        }
    }

    private fun formatSelector(selector: Selector): String =
        buildString {
            append(if (selector.anyNamespace) "*" else selector.traceNamespace)
            append('.')
            selector.channelTokens.forEachIndexed { index, token ->
                if (index > 0) {
                    append('.')
                }
                append(token)
            }
        }
}
