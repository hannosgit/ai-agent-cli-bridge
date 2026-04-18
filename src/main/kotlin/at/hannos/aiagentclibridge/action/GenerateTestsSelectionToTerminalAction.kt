package at.hannos.aiagentclibridge.action

class GenerateTestsSelectionToTerminalAction : SendSelectionToTerminalAction() {
    override fun getPromptText(): String = "Generate tests"
    override fun executeCommand(): Boolean = true
}