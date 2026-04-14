package at.hannos.aiagentclibridge

import com.intellij.terminal.ui.TerminalWidget

class ClassicAiTerminal(val terminalToolWindowTab: TerminalWidget?) : AiTerminal {

    override fun sendText(text: String) {
        terminalToolWindowTab?.ttyConnector?.write(text)
    }
}