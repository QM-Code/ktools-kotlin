package kcli.internal

import kcli.FlagHandler
import kcli.PositionalHandler
import kcli.ValueHandler

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
