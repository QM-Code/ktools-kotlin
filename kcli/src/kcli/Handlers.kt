package kcli

fun interface FlagHandler {
    @Throws(Exception::class)
    fun handle(context: HandlerContext)
}

fun interface ValueHandler {
    @Throws(Exception::class)
    fun handle(context: HandlerContext, value: String)
}

fun interface PositionalHandler {
    @Throws(Exception::class)
    fun handle(context: HandlerContext)
}
