package at.hannos.aiagentclibridge.action

import at.hannos.aiagentclibridge.action.TerminalActionSupport.buildReference
import at.hannos.aiagentclibridge.action.TerminalActionSupport.terminalIsFound
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys


class SendFileReferenceToTerminalAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val virtualFiles = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
            ?.takeIf { it.isNotEmpty() }
            ?: event.getData(CommonDataKeys.VIRTUAL_FILE)?.let { arrayOf(it) }

        if (virtualFiles.isNullOrEmpty()) {
            TerminalActionSupport.notifyError(project, "Failed to resolve selected file or folder")
            return
        }

        val command = virtualFiles
            .distinctBy { it.path }
            .joinToString(separator = "") { buildReference(null, null, project, it) }

        TerminalActionSupport.sendToTerminal(project, command, false)
    }

    override fun update(event: AnActionEvent) {
        val files = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        val singleFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
        if (files.isNullOrEmpty() && singleFile === null) {
            return
        }

        event.presentation.isEnabledAndVisible = terminalIsFound(event)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}

