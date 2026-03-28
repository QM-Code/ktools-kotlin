package kcli.internal

import kcli.HandlerContext

object ParseEngine {
    fun parseOrExit(data: ParserData, argc: Int, argv: Array<String>?) {
        try {
            parse(data, argc, argv)
        } catch (ex: kcli.CliError) {
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
                            help.helpRows.addAll(HelpRendering.buildHelpRows(parser))
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
                HelpRendering.printHelp(invocation)
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
            } catch (_: Throwable) {
                result.reportError(
                    invocation.option,
                    formatOptionErrorMessage(invocation.option, "unknown exception while handling option"),
                )
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
