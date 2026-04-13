package at.hannos.aiagentclibridge

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.ui.TerminalWidget
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.nio.file.Path


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
            notifyError(project, "Failed to resolve selected file")
            return
        }

        val selectionStart = selectionModel.selectionStart
        val selectionEnd = selectionModel.selectionEnd
        val endOffsetForLine = (selectionEnd - 1).coerceAtLeast(selectionStart)
        val startLine = editor.document.getLineNumber(selectionStart) + 1
        val endLine = editor.document.getLineNumber(endOffsetForLine) + 1
        val filePath = toProjectRelativePath(project, virtualFile.path)
        val command = "@${filePath}#L${startLine}-${endLine}"

        try {
            val terminalManager = TerminalToolWindowManager.getInstance(project)
            val workingDirectory = project.basePath
            val settings = AiAgentCliBridgeSettings.getInstance().state
            val terminalTitle = settings.terminalTitle
            val launchProgramWhenNoTerminalFound = settings.launchProgramWhenNoTerminalFound.trim()
            val existingTerminal = findTerminalWidgetByTitle(terminalManager, terminalTitle)
            val terminalWidget = existingTerminal
                ?: terminalManager.createShellWidget(workingDirectory, terminalTitle, true, false)

            if (existingTerminal == null && launchProgramWhenNoTerminalFound.isNotBlank()) {
                terminalWidget.sendCommandToExecute(launchProgramWhenNoTerminalFound)
            }

            if (!sendTextWithoutExecuting(terminalWidget, command)) {
                notifyError(project, "Failed to insert selection reference into terminal prompt")
                return
            }
        } catch (_: Exception) {
            notifyError(project, "Failed to send selection reference to terminal")
        }
    }

    override fun update(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        val hasSelection = editor?.selectionModel?.hasSelection() == true
        event.presentation.isEnabledAndVisible = event.project != null && hasSelection
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    private fun toProjectRelativePath(project: Project, filePath: String): String {
        val basePath = project.basePath ?: return filePath
        return try {
            val relative = Path.of(basePath).normalize().relativize(Path.of(filePath).normalize())
            relative.toString().replace('\\', '/')
        } catch (_: Exception) {
            filePath
        }
    }

    private fun notifyError(project: Project, message: String) {
        Notifications.Bus.notify(
            Notification(
                "ai-agent-cli-bridge",
                "AI Agent CLI Bridge",
                message,
                NotificationType.ERROR,
            ),
            project,
        )
    }

    private fun sendTextWithoutExecuting(terminalWidget: TerminalWidget, text: String): Boolean {
        val connector = terminalWidget.ttyConnectorAccessor.ttyConnector ?: return false
        return try {
            connector.write(text)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun findTerminalWidgetByTitle(
        terminalManager: TerminalToolWindowManager,
        title: String,
    ): TerminalWidget? {
        return terminalManager.terminalWidgets.find {
            it.terminalTitle.buildFullTitle() == title
        }
    }
}