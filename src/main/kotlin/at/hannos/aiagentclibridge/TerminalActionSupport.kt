package at.hannos.aiagentclibridge

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.nio.file.Path


object TerminalActionSupport {

    fun sendToTerminal(project: Project, command: String) {
        try {
            val settings = AiAgentCliBridgeSettings.getInstance().state
            val terminalTitle = settings.terminalTitle
            val windowTab =
                findTerminalWidgetByTitle(project, terminalTitle) ?: return

            windowTab.sendText(command)
        } catch (_: Exception) {
            notifyError(project, "Failed to send selection reference to terminal")
        }
    }

    fun toProjectRelativePath(project: Project, filePath: String): String {
        val basePath = project.basePath ?: return filePath
        return try {
            val relative = Path.of(basePath).normalize().relativize(Path.of(filePath).normalize())
            relative.toString().replace('\\', '/')
        } catch (_: Exception) {
            filePath
        }
    }

    fun notifyError(project: Project, message: String) {
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

    fun terminalIsFound(event: AnActionEvent): Boolean {
        val project = event.project
        if (project === null) {
            return false
        }
        val hasConfiguredTerminal = run {
            val terminalTitle = AiAgentCliBridgeSettings.getInstance().state.terminalTitle
            hasTerminalWithTitle(project, terminalTitle)
        }
        return hasConfiguredTerminal
    }

    private fun findTerminalWidgetByTitle(
        project: Project,
        title: String,
    ): AiTerminal? {
        // Reworked Terminal
        val tabs = TerminalToolWindowTabsManager.getInstance(project).tabs
        val found = tabs.find { it.view.title.buildFullTitle() == title }
        if (found !== null) {
            return ReworkedAiTerminal(found)
        }

        // Classic Terminal
        val terminalWidget =
            TerminalToolWindowManager.getInstance(project).terminalWidgets.find { it.terminalTitle.buildFullTitle() == title }
        if (terminalWidget !== null) {
            return ClassicAiTerminal(terminalWidget)
        }

        return null
    }

    private fun hasTerminalWithTitle(project: Project, title: String): Boolean {
        val findTerminalWidgetByTitle = findTerminalWidgetByTitle(project, title)

        return findTerminalWidgetByTitle !== null
    }

    fun buildReference(
        selectionModel: SelectionModel?,
        editor: Editor?,
        project: Project,
        virtualFile: VirtualFile
    ): String {
        val filePath = toProjectRelativePath(project, virtualFile.path)

        if (selectionModel == null || editor == null) {
            return "@${filePath} "
        }

        val selectionStart = selectionModel.selectionStart
        val selectionEnd = selectionModel.selectionEnd
        val endOffsetForLine = (selectionEnd - 1).coerceAtLeast(selectionStart)
        val startLine = editor.document.getLineNumber(selectionStart) + 1
        val endLine = editor.document.getLineNumber(endOffsetForLine) + 1

        if (startLine == endLine) {
            return "@${filePath}#L${startLine} "
        }

        return "@${filePath}#L${startLine}-${endLine} "
    }

}