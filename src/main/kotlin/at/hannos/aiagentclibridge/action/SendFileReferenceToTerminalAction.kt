package at.hannos.aiagentclibridge.action

import at.hannos.aiagentclibridge.action.TerminalActionSupport
import at.hannos.aiagentclibridge.action.TerminalActionSupport.buildReference
import at.hannos.aiagentclibridge.action.TerminalActionSupport.terminalIsFound
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys


class SendFileReferenceToTerminalAction : AnAction("Send File Reference to AI Tool") {

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
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
        if (virtualFile === null) {
            return
        }

        event.presentation.isEnabledAndVisible = terminalIsFound(event)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}

