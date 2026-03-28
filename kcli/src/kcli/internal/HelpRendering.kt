package kcli.internal

object HelpRendering {
    fun printHelp(invocation: Invocation) {
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

    fun buildHelpRows(parser: InlineParserData): List<HelpRow> {
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
}
