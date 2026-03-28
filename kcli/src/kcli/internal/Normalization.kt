package kcli.internal

import kcli.CliError

object Normalization {
    fun reportCliErrorAndExit(message: String?) {
        val text = message ?: ""
        if (System.console() != null) {
            System.err.printf("[\u001b[31merror\u001b[0m] [\u001b[94mcli\u001b[0m] %s%n", text)
        } else {
            System.err.printf("[error] [cli] %s%n", text)
        }
        System.err.flush()
        System.exit(2)
    }

    fun startsWith(value: String, prefix: String): Boolean = value.startsWith(prefix)

    fun trimWhitespace(value: String?): String = value?.trim().orEmpty()

    private fun containsWhitespace(value: String): Boolean = value.any { it.isWhitespace() }

    private fun normalizeRootNameOrThrow(rawRoot: String): String {
        val root = trimWhitespace(rawRoot)
        require(root.isNotEmpty()) { "kcli root must not be empty" }
        require(root[0] != '-') { "kcli root must not begin with '-'" }
        require(!containsWhitespace(root)) { "kcli root is invalid" }
        return root
    }

    fun normalizeInlineRootOptionOrThrow(rawRoot: String): String {
        var root = trimWhitespace(rawRoot)
        require(root.isNotEmpty()) { "kcli root must not be empty" }
        root = when {
            startsWith(root, "--") -> root.substring(2)
            root[0] == '-' -> throw IllegalArgumentException("kcli root must use '--root' or 'root'")
            else -> root
        }
        return normalizeRootNameOrThrow(root)
    }

    fun normalizeInlineHandlerOptionOrThrow(rawOption: String, rootName: String): String {
        var option = trimWhitespace(rawOption)
        require(option.isNotEmpty()) { "kcli inline handler option must not be empty" }
        option = when {
            startsWith(option, "--") -> {
                val fullPrefix = "--$rootName-"
                require(startsWith(option, fullPrefix)) {
                    "kcli inline handler option must use '-name' or '${fullPrefix}name'"
                }
                option.substring(fullPrefix.length)
            }
            option[0] == '-' -> option.substring(1)
            else -> throw IllegalArgumentException(
                "kcli inline handler option must use '-name' or '--$rootName-name'",
            )
        }
        require(option.isNotEmpty()) { "kcli command must not be empty" }
        require(option[0] != '-') { "kcli command must not start with '-'" }
        require(!containsWhitespace(option)) { "kcli command must not contain whitespace" }
        return option
    }

    fun normalizePrimaryHandlerOptionOrThrow(rawOption: String): String {
        var option = trimWhitespace(rawOption)
        require(option.isNotEmpty()) { "kcli end-user handler option must not be empty" }
        option = when {
            startsWith(option, "--") -> option.substring(2)
            option[0] == '-' -> throw IllegalArgumentException(
                "kcli end-user handler option must use '--name' or 'name'",
            )
            else -> option
        }
        require(option.isNotEmpty()) { "kcli command must not be empty" }
        require(option[0] != '-') { "kcli command must not start with '-'" }
        require(!containsWhitespace(option)) { "kcli command must not contain whitespace" }
        return option
    }

    fun normalizeAliasOrThrow(rawAlias: String): String {
        val alias = trimWhitespace(rawAlias)
        require(alias.length >= 2 && alias[0] == '-' && !startsWith(alias, "--") && !containsWhitespace(alias)) {
            "kcli alias must use single-dash form, e.g. '-v'"
        }
        return alias
    }

    fun normalizeAliasTargetOptionOrThrow(rawTarget: String): String {
        val target = trimWhitespace(rawTarget)
        require(target.length >= 3 && startsWith(target, "--") && !containsWhitespace(target)) {
            "kcli alias target must use double-dash form, e.g. '--verbose'"
        }
        require(target[2] != '-') {
            "kcli alias target must use double-dash form, e.g. '--verbose'"
        }
        return target
    }

    fun normalizeHelpPlaceholderOrThrow(rawPlaceholder: String): String {
        val placeholder = trimWhitespace(rawPlaceholder)
        require(placeholder.isNotEmpty()) { "kcli help placeholder must not be empty" }
        return placeholder
    }

    fun normalizeDescriptionOrThrow(rawDescription: String): String {
        val description = trimWhitespace(rawDescription)
        require(description.isNotEmpty()) { "kcli command description must not be empty" }
        return description
    }

    fun throwCliError(result: MutableParseOutcome): Nothing {
        check(!result.ok) { "kcli internal error: throwCliError called without a failure" }
        throw CliError(result.errorOption, result.errorMessage)
    }
}
