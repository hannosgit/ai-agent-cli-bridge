package at.hannos.aiagentclibridge.action

import at.hannos.aiagentclibridge.config.AiAgentCliBridgeSettings
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory


class CreateConfiguredTerminalSessionAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        if (TerminalActionSupport.terminalIsFound(event)) {
            showTerminal(project)
        } else {
            launchAiToolInTerminal(project)
        }
    }

    private fun showTerminal(project: Project) {
        val toolWindow = ToolWindowManager.getInstance(project)
            .getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID) ?: return
        val settings = AiAgentCliBridgeSettings.getInstance().state
        val contentManager = toolWindow.contentManager
        val terminalTitle = settings.terminalTitle
        val existing = contentManager.findContent(terminalTitle)

        if (existing != null) {
            // Tab exists — just focus it
            toolWindow.activate {
                contentManager.setSelectedContent(existing, true)
            }
        }
    }

    private fun launchAiToolInTerminal(project: Project) {
        val settings = AiAgentCliBridgeSettings.getInstance().state
        val terminalTitle = settings.terminalTitle
        val launchProgram = settings.launchProgramWhenNoTerminalFound

        val toolWindowTab = TerminalToolWindowTabsManager.getInstance(project).createTabBuilder()
            .workingDirectory(project.basePath)
            .tabName(terminalTitle)
            .requestFocus(true)
            .deferSessionStartUntilUiShown(true)
            .createTab()
        toolWindowTab.view.createSendTextBuilder().shouldExecute().send(launchProgram)

    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
