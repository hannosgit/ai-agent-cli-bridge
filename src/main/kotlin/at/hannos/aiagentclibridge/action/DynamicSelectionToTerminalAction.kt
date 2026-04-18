package at.hannos.aiagentclibridge.action

class DynamicSelectionToTerminalAction(
    actionText: String,
    private val prompt: String,
) : SendSelectionToTerminalAction(actionText) {

    override fun getPromptText(): String = prompt

    override fun executeCommand(): Boolean = true
}
