package at.hannos.aiagentclibridge

import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTab

class ReworkedAiTerminal(private val terminalToolWindowTab: TerminalToolWindowTab) : AiTerminal {

    override fun sendText(text: String): Boolean {
        terminalToolWindowTab.view.sendText(text)
        return true
    }
}