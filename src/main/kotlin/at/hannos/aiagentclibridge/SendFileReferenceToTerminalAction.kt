package at.hannos.aiagentclibridge

import at.hannos.aiagentclibridge.TerminalActionSupport.buildReference
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import org.jetbrains.plugins.terminal.TerminalToolWindowManager


class SendFileReferenceToTerminalAction : AnAction("Send File Reference to Terminal") {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)

        if (virtualFile == null) {
            TerminalActionSupport.notifyError(project, "Failed to resolve selected file or folder")
            return
        }

        val command = buildReference(null, null, project, virtualFile)

        try {
            val terminalManager = TerminalToolWindowManager.getInstance(project)
            val workingDirectory = project.basePath
            val settings = AiAgentCliBridgeSettings.getInstance().state
            val terminalTitle = settings.terminalTitle
            val launchProgramWhenNoTerminalFound = settings.launchProgramWhenNoTerminalFound.trim()
            val existingTerminal = TerminalActionSupport.findTerminalWidgetByTitle(terminalManager, terminalTitle)
            val terminalWidget = existingTerminal
                ?: terminalManager.createShellWidget(workingDirectory, terminalTitle, true, false)

            if (existingTerminal == null && launchProgramWhenNoTerminalFound.isNotBlank()) {
                terminalWidget.sendCommandToExecute(launchProgramWhenNoTerminalFound)
            }

            if (!TerminalActionSupport.sendTextWithoutExecuting(terminalWidget, command)) {
                TerminalActionSupport.notifyError(project, "Failed to insert file reference into terminal prompt")
                return
            }
        } catch (_: Exception) {
            TerminalActionSupport.notifyError(project, "Failed to send file reference to terminal")
        }
    }

    override fun update(event: AnActionEvent) {
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
        event.presentation.isEnabledAndVisible = event.project != null && virtualFile != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}