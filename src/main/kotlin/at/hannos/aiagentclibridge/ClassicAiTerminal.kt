package at.hannos.aiagentclibridge

import com.intellij.terminal.ui.TerminalWidget

class ClassicAiTerminal(private val terminalWidget: TerminalWidget) : AiTerminal {

    override fun sendText(text: String) {
        terminalWidget.ttyConnector?.write(text)
    }
}