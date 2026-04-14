package at.hannos.aiagentclibridge

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.jetbrains.plugins.terminal.TerminalToolWindowManager


class CreateConfiguredTerminalSessionAction : AnAction("Open Configured AI Terminal") {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val settings = AiAgentCliBridgeSettings.getInstance().state
        val terminalTitle = settings.terminalTitle
        val launchProgram = settings.launchProgramWhenNoTerminalFound

        val terminalWidget = TerminalToolWindowManager.getInstance(project)
            .createShellWidget(project.basePath, terminalTitle, true, true)

        if (launchProgram.isNotBlank()) {
            terminalWidget.sendCommandToExecute(launchProgram)
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
