package at.hannos.aiagentclibridge

import at.hannos.aiagentclibridge.TerminalActionSupport.buildReference
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys


class SendFileReferenceToTerminalAction : AnAction("Send File Reference to Terminal") {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)

        if (virtualFile == null) {
            TerminalActionSupport.notifyError(project, "Failed to resolve selected file or folder")
            return
        }

        val command = buildReference(null, null, project, virtualFile)
        TerminalActionSupport.sendToTerminal(project, command)
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val hasConfiguredTerminal = if (project != null) {
            val terminalTitle = AiAgentCliBridgeSettings.getInstance().state.terminalTitle
            TerminalActionSupport.hasTerminalWithTitle(project, terminalTitle)
        } else {
            false
        }

        event.presentation.isEnabledAndVisible = project != null && virtualFile != null && hasConfiguredTerminal
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}