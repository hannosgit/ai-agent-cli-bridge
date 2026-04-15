package at.hannos.aiagentclibridge

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread

class AiReviewAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        TerminalActionSupport.sendCommandToTerminal(project, "/review")
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = TerminalActionSupport.terminalIsFound(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}