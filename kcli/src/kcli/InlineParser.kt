package kcli

import kcli.internal.InlineParserData
import kcli.internal.Registration

class InlineParser(root: String) {
    private val data = InlineParserData()

    init {
        Registration.setInlineRoot(data, root)
    }

    fun setRoot(root: String) {
        Registration.setInlineRoot(data, root)
    }

    fun setRootValueHandler(handler: ValueHandler) {
        Registration.setRootValueHandler(data, handler)
    }

    fun setRootValueHandler(handler: ValueHandler, valuePlaceholder: String, description: String) {
        Registration.setRootValueHandler(data, handler, valuePlaceholder, description)
    }

    fun setHandler(option: String, handler: FlagHandler, description: String) {
        Registration.setInlineHandler(data, option, handler, description)
    }

    fun setHandler(option: String, handler: ValueHandler, description: String) {
        Registration.setInlineHandler(data, option, handler, description)
    }

    fun setOptionalValueHandler(option: String, handler: ValueHandler, description: String) {
        Registration.setInlineOptionalValueHandler(data, option, handler, description)
    }

    fun copy(): InlineParser {
        val copy = InlineParser("--placeholder")
        copy.data.copyFrom(data.copy())
        return copy
    }

    internal fun snapshot(): InlineParserData = data.copy()
}
