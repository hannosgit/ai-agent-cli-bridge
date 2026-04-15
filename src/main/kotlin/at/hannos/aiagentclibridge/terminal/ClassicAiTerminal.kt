package at.hannos.aiagentclibridge.terminal

import com.intellij.terminal.ui.TerminalWidget

class ClassicAiTerminal(private val terminalWidget: TerminalWidget) : AiTerminal {

    override fun sendText(text: String): Boolean {
        val connector = terminalWidget.ttyConnector ?: return false
        connector.write(text)
        return true
    }

    override fun sendTextAndExecute(text: String) {
        terminalWidget.sendCommandToExecute(text)
    }
}