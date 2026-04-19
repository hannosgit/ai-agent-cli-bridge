package at.hannos.aiagentclibridge.action

import at.hannos.aiagentclibridge.config.AiAgentCliBridgeSettings
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager


class CreateConfiguredTerminalSessionAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return

        if (TerminalActionSupport.terminalIsFound(event)) {
            sendToTerminal(event, project)
        } else {
            launchAiToolInTerminal(project)
        }
    }

    private fun sendToTerminal(
        event: AnActionEvent,
        project: Project
    ) {
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
            ?: FileEditorManager.getInstance(project).selectedFiles.firstOrNull()

        if (virtualFile != null) {
            val command = TerminalActionSupport.buildReference(null, null, project, virtualFile)
            TerminalActionSupport.sendToTerminal(project, command, false)
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
