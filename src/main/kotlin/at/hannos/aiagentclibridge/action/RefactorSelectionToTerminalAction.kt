package at.hannos.aiagentclibridge.action

class RefactorSelectionToTerminalAction : SendSelectionToTerminalAction() {
    override fun getPromptText(): String = "Refactor"
    override fun executeCommand(): Boolean = true
}