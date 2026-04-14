package at.hannos.aiagentclibridge

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTab
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager
import java.nio.file.Path


object TerminalActionSupport {

    fun sendToTerminal(project: Project, command: String) {
        try {
            val settings = AiAgentCliBridgeSettings.getInstance().state
            val terminalTitle = settings.terminalTitle
            val windowTab =
                findTerminalWidgetByTitle(project, terminalTitle) ?: return

            windowTab.view.sendText(command)
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

    fun findTerminalWidgetByTitle(
        project: Project,
        title: String,
    ): TerminalToolWindowTab? {
        val tabs = TerminalToolWindowTabsManager.getInstance(project).tabs
        return tabs.find { it.view.title.buildFullTitle() == title }
    }

    fun hasTerminalWithTitle(project: Project, title: String): Boolean {
        val tabs = TerminalToolWindowTabsManager.getInstance(project).tabs
        return tabs.any { it.view.title.buildFullTitle() == title }
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