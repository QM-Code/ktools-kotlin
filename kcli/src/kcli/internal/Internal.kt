package kcli.internal

import kcli.CliError
import kcli.FlagHandler
import kcli.HandlerContext
import kcli.PositionalHandler
import kcli.ValueHandler

enum class ValueArity {
    REQUIRED,
    OPTIONAL,
}

class CommandBinding {
    var expectsValue = false
    var flagHandler: FlagHandler? = null
    var valueHandler: ValueHandler? = null
    var valueArity: ValueArity = ValueArity.REQUIRED
    var description = ""

    fun copy(): CommandBinding {
        val copy = CommandBinding()
        copy.expectsValue = expectsValue
        copy.flagHandler = flagHandler
        copy.valueHandler = valueHandler
        copy.valueArity = valueArity
        copy.description = description
        return copy
    }
}

class AliasBinding {
    var alias = ""
    var targetToken = ""
    val presetTokens = mutableListOf<String>()

    fun copy(): AliasBinding {
        val copy = AliasBinding()
        copy.alias = alias
        copy.targetToken = targetToken
        copy.presetTokens.addAll(presetTokens)
        return copy
    }
}

class MutableParseOutcome {
    var ok = true
    var errorOption = ""
    var errorMessage = ""

    fun reportError(option: String?, message: String?) {
        if (!ok) {
            return
        }

        ok = false
        errorOption = option ?: ""
        errorMessage = message ?: ""
    }
}

class CollectedValues {
    var hasValue = false
    val parts = mutableListOf<String>()
    var lastIndex = -1
}

enum class InvocationKind {
    FLAG,
    VALUE,
    POSITIONAL,
    PRINT_HELP,
}

class Invocation {
    var kind = InvocationKind.FLAG
    var root = ""
    var option = ""
    var command = ""
    val valueTokens = mutableListOf<String>()
    var flagHandler: FlagHandler? = null
    var valueHandler: ValueHandler? = null
    var positionalHandler: PositionalHandler? = null
    val helpRows = mutableListOf<HelpRow>()
}

data class HelpRow(val lhs: String, val rhs: String)

enum class InlineTokenKind {
    NONE,
    BARE_ROOT,
    DASH_OPTION,
}

class InlineTokenMatch {
    var kind = InlineTokenKind.NONE
    var parser: InlineParserData? = null
    var suffix = ""
}

class InlineParserData {
    var rootName = ""
    var rootValueHandler: ValueHandler? = null
    var rootValuePlaceholder = ""
    var rootValueDescription = ""
    val commands = linkedMapOf<String, CommandBinding>()

    fun copy(): InlineParserData {
        val copy = InlineParserData()
        copy.rootName = rootName
        copy.rootValueHandler = rootValueHandler
        copy.rootValuePlaceholder = rootValuePlaceholder
        copy.rootValueDescription = rootValueDescription
        for ((key, value) in commands) {
            copy.commands[key] = value.copy()
        }
        return copy
    }

    fun copyFrom(other: InlineParserData) {
        rootName = other.rootName
        rootValueHandler = other.rootValueHandler
        rootValuePlaceholder = other.rootValuePlaceholder
        rootValueDescription = other.rootValueDescription
        commands.clear()
        for ((key, value) in other.commands) {
            commands[key] = value.copy()
        }
    }
}

class ParserData {
    var positionalHandler: PositionalHandler? = null
    val aliases = linkedMapOf<String, AliasBinding>()
    val commands = linkedMapOf<String, CommandBinding>()
    val inlineParsers = linkedMapOf<String, InlineParserData>()
}

object Registration {
    fun setInlineRoot(data: InlineParserData, root: String) {
        data.rootName = Normalization.normalizeInlineRootOptionOrThrow(root)
    }

    fun setRootValueHandler(data: InlineParserData, handler: ValueHandler) {
        data.rootValueHandler = handler
        data.rootValuePlaceholder = ""
        data.rootValueDescription = ""
    }

    fun setRootValueHandler(
        data: InlineParserData,
        handler: ValueHandler,
        valuePlaceholder: String,
        description: String,
    ) {
        data.rootValueHandler = handler
        data.rootValuePlaceholder = Normalization.normalizeHelpPlaceholderOrThrow(valuePlaceholder)
        data.rootValueDescription = Normalization.normalizeDescriptionOrThrow(description)
    }

    fun setInlineHandler(data: InlineParserData, option: String, handler: FlagHandler, description: String) {
        val command = Normalization.normalizeInlineHandlerOptionOrThrow(option, data.rootName)
        data.commands[command] = makeFlagBinding(handler, description)
    }

    fun setInlineHandler(data: InlineParserData, option: String, handler: ValueHandler, description: String) {
        val command = Normalization.normalizeInlineHandlerOptionOrThrow(option, data.rootName)
        data.commands[command] = makeValueBinding(handler, description, ValueArity.REQUIRED)
    }

    fun setInlineOptionalValueHandler(
        data: InlineParserData,
        option: String,
        handler: ValueHandler,
        description: String,
    ) {
        val command = Normalization.normalizeInlineHandlerOptionOrThrow(option, data.rootName)
        data.commands[command] = makeValueBinding(handler, description, ValueArity.OPTIONAL)
    }

    fun setAlias(data: ParserData, alias: String, target: String, vararg presetTokens: String) {
        val normalizedAlias = Normalization.normalizeAliasOrThrow(alias)
        val normalizedTarget = Normalization.normalizeAliasTargetOptionOrThrow(target)
        val binding = AliasBinding()
        binding.alias = normalizedAlias
        binding.targetToken = normalizedTarget
        binding.presetTokens.addAll(presetTokens)
        data.aliases[normalizedAlias] = binding
    }

    fun setPrimaryHandler(data: ParserData, option: String, handler: FlagHandler, description: String) {
        val command = Normalization.normalizePrimaryHandlerOptionOrThrow(option)
        data.commands[command] = makeFlagBinding(handler, description)
    }

    fun setPrimaryHandler(data: ParserData, option: String, handler: ValueHandler, description: String) {
        val command = Normalization.normalizePrimaryHandlerOptionOrThrow(option)
        data.commands[command] = makeValueBinding(handler, description, ValueArity.REQUIRED)
    }

    fun setPrimaryOptionalValueHandler(
        data: ParserData,
        option: String,
        handler: ValueHandler,
        description: String,
    ) {
        val command = Normalization.normalizePrimaryHandlerOptionOrThrow(option)
        data.commands[command] = makeValueBinding(handler, description, ValueArity.OPTIONAL)
    }

    fun setPositionalHandler(data: ParserData, handler: PositionalHandler) {
        data.positionalHandler = handler
    }

    fun addInlineParser(data: ParserData, parser: InlineParserData) {
        require(!data.inlineParsers.containsKey(parser.rootName)) {
            "kcli inline parser root '--${parser.rootName}' is already registered"
        }
        data.inlineParsers[parser.rootName] = parser.copy()
    }

    private fun makeFlagBinding(handler: FlagHandler, description: String): CommandBinding {
        val binding = CommandBinding()
        binding.expectsValue = false
        binding.flagHandler = handler
        binding.description = Normalization.normalizeDescriptionOrThrow(description)
        return binding
    }

    private fun makeValueBinding(handler: ValueHandler, description: String, arity: ValueArity): CommandBinding {
        val binding = CommandBinding()
        binding.expectsValue = true
        binding.valueHandler = handler
        binding.valueArity = arity
        binding.description = Normalization.normalizeDescriptionOrThrow(description)
        return binding
    }
}

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

object ParseEngine {
    fun parseOrExit(data: ParserData, argc: Int, argv: Array<String>?) {
        try {
            parse(data, argc, argv)
        } catch (ex: CliError) {
            Normalization.reportCliErrorAndExit(ex.message)
        }
    }

    fun parse(data: ParserData, argc: Int, argv: Array<String>?) {
        val result = MutableParseOutcome()
        if (argc > 0 && argv == null) {
            result.reportError("", "kcli received invalid argv (argc > 0 but argv is null)")
            Normalization.throwCliError(result)
        }

        if (argc <= 0 || argv == null) {
            return
        }

        if (argv.size < argc) {
            result.reportError("", "kcli received invalid argv (argv shorter than argc)")
            Normalization.throwCliError(result)
        }

        val consumed = BooleanArray(argc)
        val invocations = mutableListOf<Invocation>()
        val tokens = buildParseTokens(argc, argv)
        var index = 1
        while (index < argc) {
            if (consumed[index]) {
                index += 1
                continue
            }

            val arg = tokens[index]
            if (arg.isEmpty()) {
                index += 1
                continue
            }

            var aliasBinding: AliasBinding? = null
            var effectiveArg = arg
            if (arg[0] == '-' && !Normalization.startsWith(arg, "--")) {
                aliasBinding = data.aliases[arg]
                if (aliasBinding != null) {
                    effectiveArg = aliasBinding.targetToken
                }
            }

            if (effectiveArg[0] != '-') {
                index += 1
                continue
            }

            if (effectiveArg == "--") {
                index += 1
                continue
            }

            if (Normalization.startsWith(effectiveArg, "--")) {
                val inlineMatch = matchInlineToken(data, effectiveArg)
                when (inlineMatch.kind) {
                    InlineTokenKind.BARE_ROOT -> {
                        consumeIndex(consumed, index)
                        val parser = inlineMatch.parser!!
                        val collected = collectValueTokens(index, tokens, consumed, allowOptionLikeFirstValue = false)
                        if (!collected.hasValue && !hasAliasPresetTokens(aliasBinding)) {
                            val help = Invocation()
                            help.kind = InvocationKind.PRINT_HELP
                            help.root = parser.rootName
                            help.helpRows.addAll(buildHelpRows(parser))
                            invocations.add(help)
                        } else if (parser.rootValueHandler == null) {
                            result.reportError(effectiveArg, "unknown value for option '$effectiveArg'")
                        } else {
                            val invocation = Invocation()
                            invocation.kind = InvocationKind.VALUE
                            invocation.root = parser.rootName
                            invocation.option = effectiveArg
                            invocation.valueHandler = parser.rootValueHandler
                            invocation.valueTokens.addAll(buildEffectiveValueTokens(aliasBinding, collected.parts))
                            invocations.add(invocation)
                            if (collected.hasValue) {
                                index = collected.lastIndex
                            }
                        }
                    }

                    InlineTokenKind.DASH_OPTION -> {
                        val parser = inlineMatch.parser!!
                        if (inlineMatch.suffix.isNotEmpty()) {
                            val binding = parser.commands[inlineMatch.suffix]
                            if (binding != null) {
                                index = scheduleInvocation(
                                    binding,
                                    aliasBinding,
                                    parser.rootName,
                                    inlineMatch.suffix,
                                    effectiveArg,
                                    index,
                                    tokens,
                                    consumed,
                                    invocations,
                                    result,
                                )
                            }
                        }
                    }

                    InlineTokenKind.NONE -> {
                        val command = effectiveArg.substring(2)
                        val binding = data.commands[command]
                        if (binding != null) {
                            index = scheduleInvocation(
                                binding,
                                aliasBinding,
                                "",
                                command,
                                effectiveArg,
                                index,
                                tokens,
                                consumed,
                                invocations,
                                result,
                            )
                        }
                    }
                }
            }

            if (!result.ok) {
                break
            }

            index += 1
        }

        if (result.ok) {
            schedulePositionals(data, tokens, consumed, invocations)
        }

        if (result.ok) {
            for (scan in 1 until argc) {
                if (consumed[scan]) {
                    continue
                }
                val token = tokens[scan]
                if (token.isNotEmpty() && token[0] == '-') {
                    result.reportError(token, "unknown option $token")
                    break
                }
            }
        }

        if (result.ok) {
            executeInvocations(invocations, result)
        }

        if (!result.ok) {
            Normalization.throwCliError(result)
        }
    }

    private fun collectValueTokens(
        optionIndex: Int,
        tokens: List<String>,
        consumed: BooleanArray,
        allowOptionLikeFirstValue: Boolean,
    ): CollectedValues {
        val collected = CollectedValues()
        collected.lastIndex = optionIndex

        val firstValueIndex = optionIndex + 1
        val hasNext = firstValueIndex in tokens.indices && !consumed[firstValueIndex]
        if (!hasNext) {
            return collected
        }

        val first = tokens[firstValueIndex]
        if (!allowOptionLikeFirstValue && first.isNotEmpty() && first[0] == '-') {
            return collected
        }

        collected.hasValue = true
        collected.parts.add(first)
        consumed[firstValueIndex] = true
        collected.lastIndex = firstValueIndex

        if (allowOptionLikeFirstValue && first.isNotEmpty() && first[0] == '-') {
            return collected
        }

        for (scan in firstValueIndex + 1 until tokens.size) {
            if (consumed[scan]) {
                continue
            }

            val next = tokens[scan]
            if (next.isNotEmpty() && next[0] == '-') {
                break
            }

            collected.parts.add(next)
            consumed[scan] = true
            collected.lastIndex = scan
        }
        return collected
    }

    private fun printHelp(invocation: Invocation) {
        println()
        println("Available --${invocation.root}-* options:")
        val maxLhs = invocation.helpRows.maxOfOrNull { it.lhs.length } ?: 0
        if (invocation.helpRows.isEmpty()) {
            println("  (no options registered)")
        } else {
            for (row in invocation.helpRows) {
                val padding = if (maxLhs > row.lhs.length) maxLhs - row.lhs.length else 0
                println("  ${row.lhs}${" ".repeat(padding + 2)}${row.rhs}")
            }
        }
        println()
    }

    private fun consumeIndex(consumed: BooleanArray, index: Int) {
        if (index in consumed.indices && !consumed[index]) {
            consumed[index] = true
        }
    }

    private fun hasAliasPresetTokens(aliasBinding: AliasBinding?): Boolean =
        aliasBinding != null && aliasBinding.presetTokens.isNotEmpty()

    private fun buildEffectiveValueTokens(aliasBinding: AliasBinding?, collectedParts: List<String>): List<String> {
        if (!hasAliasPresetTokens(aliasBinding)) {
            return collectedParts.toList()
        }
        return buildList {
            addAll(aliasBinding!!.presetTokens)
            addAll(collectedParts)
        }
    }

    private fun buildHelpRows(parser: InlineParserData): List<HelpRow> {
        val rows = mutableListOf<HelpRow>()
        if (parser.rootValueHandler != null && parser.rootValueDescription.isNotEmpty()) {
            var lhs = "--${parser.rootName}"
            if (parser.rootValuePlaceholder.isNotEmpty()) {
                lhs += " ${parser.rootValuePlaceholder}"
            }
            rows.add(HelpRow(lhs, parser.rootValueDescription))
        }
        for ((key, binding) in parser.commands) {
            var lhs = "--${parser.rootName}-$key"
            if (binding.expectsValue) {
                lhs += when (binding.valueArity) {
                    ValueArity.OPTIONAL -> " [value]"
                    ValueArity.REQUIRED -> " <value>"
                }
            }
            rows.add(HelpRow(lhs, binding.description))
        }
        return rows
    }

    private fun matchInlineToken(data: ParserData, arg: String): InlineTokenMatch {
        for (parser in data.inlineParsers.values) {
            val rootOption = "--${parser.rootName}"
            if (arg == rootOption) {
                return InlineTokenMatch().also {
                    it.kind = InlineTokenKind.BARE_ROOT
                    it.parser = parser
                }
            }
            val rootDashPrefix = "$rootOption-"
            if (Normalization.startsWith(arg, rootDashPrefix)) {
                return InlineTokenMatch().also {
                    it.kind = InlineTokenKind.DASH_OPTION
                    it.parser = parser
                    it.suffix = arg.substring(rootDashPrefix.length)
                }
            }
        }
        return InlineTokenMatch()
    }

    private fun scheduleInvocation(
        binding: CommandBinding,
        aliasBinding: AliasBinding?,
        root: String,
        command: String,
        optionToken: String,
        index: Int,
        tokens: List<String>,
        consumed: BooleanArray,
        invocations: MutableList<Invocation>,
        result: MutableParseOutcome,
    ): Int {
        var nextIndex = index
        consumeIndex(consumed, index)
        val invocation = Invocation()
        invocation.root = root
        invocation.option = optionToken
        invocation.command = command

        if (!binding.expectsValue) {
            if (hasAliasPresetTokens(aliasBinding)) {
                result.reportError(
                    aliasBinding!!.alias,
                    "alias '${aliasBinding.alias}' presets values for option '$optionToken' which does not accept values",
                )
                return nextIndex
            }

            invocation.kind = InvocationKind.FLAG
            invocation.flagHandler = binding.flagHandler
            invocations.add(invocation)
            return nextIndex
        }

        val collected = collectValueTokens(
            index,
            tokens,
            consumed,
            allowOptionLikeFirstValue = binding.valueArity == ValueArity.REQUIRED,
        )
        if (!collected.hasValue && !hasAliasPresetTokens(aliasBinding) && binding.valueArity == ValueArity.REQUIRED) {
            result.reportError(optionToken, "option '$optionToken' requires a value")
            return nextIndex
        }

        if (collected.hasValue) {
            nextIndex = collected.lastIndex
        }

        invocation.kind = InvocationKind.VALUE
        invocation.valueHandler = binding.valueHandler
        invocation.valueTokens.addAll(buildEffectiveValueTokens(aliasBinding, collected.parts))
        invocations.add(invocation)
        return nextIndex
    }

    private fun schedulePositionals(
        data: ParserData,
        tokens: List<String>,
        consumed: BooleanArray,
        invocations: MutableList<Invocation>,
    ) {
        val handler = data.positionalHandler ?: return
        if (tokens.size <= 1) {
            return
        }
        val invocation = Invocation()
        invocation.kind = InvocationKind.POSITIONAL
        invocation.positionalHandler = handler
        for (index in 1 until tokens.size) {
            if (consumed[index]) {
                continue
            }
            val token = tokens[index]
            if (token.isEmpty() || token[0] != '-') {
                consumed[index] = true
                invocation.valueTokens.add(token)
            }
        }
        if (invocation.valueTokens.isNotEmpty()) {
            invocations.add(invocation)
        }
    }

    private fun buildParseTokens(argc: Int, argv: Array<String>): List<String> =
        List(argc) { index -> argv[index] }

    private fun executeInvocations(invocations: List<Invocation>, result: MutableParseOutcome) {
        for (invocation in invocations) {
            if (!result.ok) {
                return
            }

            if (invocation.kind == InvocationKind.PRINT_HELP) {
                printHelp(invocation)
                continue
            }

            val context = HandlerContext(
                root = invocation.root,
                option = invocation.option,
                command = invocation.command,
                valueTokens = invocation.valueTokens.toList(),
            )

            try {
                when (invocation.kind) {
                    InvocationKind.FLAG -> invocation.flagHandler!!.handle(context)
                    InvocationKind.VALUE -> invocation.valueHandler!!.handle(context, invocation.valueTokens.joinToString(" "))
                    InvocationKind.POSITIONAL -> invocation.positionalHandler!!.handle(context)
                    InvocationKind.PRINT_HELP -> Unit
                }
            } catch (ex: Exception) {
                result.reportError(invocation.option, formatOptionErrorMessage(invocation.option, ex.message))
            } catch (ex: Throwable) {
                result.reportError(invocation.option, formatOptionErrorMessage(invocation.option, "unknown exception while handling option"))
            }
        }
    }

    private fun formatOptionErrorMessage(option: String, message: String?): String {
        if (option.isEmpty()) {
            return message.orEmpty()
        }
        return "option '$option': ${message.orEmpty()}"
    }
}
