package at.hannos.aiagentclibridge

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.terminal.ui.TerminalWidget
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.nio.file.Path


object TerminalActionSupport {

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
        terminalManager: TerminalToolWindowManager,
        title: String,
    ): TerminalWidget? {
        return terminalManager.terminalWidgets.find {
            it.terminalTitle.buildFullTitle() == title
        }
    }

    fun sendTextWithoutExecuting(terminalWidget: TerminalWidget, text: String): Boolean {
        val connector = terminalWidget.ttyConnector ?: return false
        return try {
            connector.write(text)
            true
        } catch (_: Exception) {
            false
        }
    }
}