package at.hannos.aiagentclibridge.terminal

import com.intellij.openapi.application.ApplicationManager
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTab

class ReworkedAiTerminal(private val terminalToolWindowTab: TerminalToolWindowTab) : AiTerminal {

    override fun sendText(text: String): Boolean {
        terminalToolWindowTab.view.sendText(text)
        requestFocus()
        return true
    }

    override fun sendTextAndExecute(text: String) {
        terminalToolWindowTab.view.createSendTextBuilder().shouldExecute().send(text)
    }

    private fun requestFocus() {
        ApplicationManager.getApplication().invokeLater {
            terminalToolWindowTab.view.preferredFocusableComponent.requestFocusInWindow()
        }
    }
}