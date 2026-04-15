package at.hannos.aiagentclibridge.action

import at.hannos.aiagentclibridge.config.AiAgentCliBridgeSettings
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class AiReviewAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val reviewCommand = AiAgentCliBridgeSettings.getInstance().state.aiReviewCommand

        TerminalActionSupport.sendCommandToTerminal(project, reviewCommand)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = TerminalActionSupport.terminalIsFound(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}