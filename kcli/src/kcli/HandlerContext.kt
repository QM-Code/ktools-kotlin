package kcli

data class HandlerContext(
    val root: String,
    val option: String,
    val command: String,
    val valueTokens: List<String>,
)
