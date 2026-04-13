package at.hannos.aiagentclibridge

import at.hannos.aiagentclibridge.TerminalActionSupport.buildReference
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager


class SendSelectionToTerminalAction : AnAction("Send Selection to Terminal") {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val selectionModel = editor.selectionModel

        if (!selectionModel.hasSelection()) {
            ToolWindowManager.getInstance(project)
                .notifyByBalloon(TerminalToolWindowFactory.TOOL_WINDOW_ID, MessageType.WARNING, "No text selected")
            return
        }

        if (virtualFile == null) {
            TerminalActionSupport.notifyError(project, "Failed to resolve selected file")
            return
        }

        val command = buildReference(selectionModel, editor, project, virtualFile)

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
                TerminalActionSupport.notifyError(project, "Failed to insert selection reference into terminal prompt")
                return
            }
        } catch (_: Exception) {
            TerminalActionSupport.notifyError(project, "Failed to send selection reference to terminal")
        }
    }

    override fun update(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        val hasSelection = editor?.selectionModel?.hasSelection() == true
        event.presentation.isEnabledAndVisible = event.project != null && hasSelection
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}