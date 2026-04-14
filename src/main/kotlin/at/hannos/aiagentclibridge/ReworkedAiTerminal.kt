package at.hannos.aiagentclibridge

import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTab

class ReworkedAiTerminal(val terminalToolWindowTab: TerminalToolWindowTab) : AiTerminal {

    override fun sendText(text: String) {
        terminalToolWindowTab.view.sendText(text)
    }
}