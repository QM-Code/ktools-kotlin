package kcli

class CliError(option: String?, message: String?) :
    RuntimeException(if (message.isNullOrEmpty()) "kcli parse failed" else message) {
    val option: String = option ?: ""
}
