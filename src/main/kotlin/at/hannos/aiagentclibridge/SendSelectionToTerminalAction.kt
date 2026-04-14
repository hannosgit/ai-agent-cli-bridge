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

        TerminalActionSupport.sendToTerminal(project, command)
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        val editor = event.getData(CommonDataKeys.EDITOR)
        val hasSelection = editor?.selectionModel?.hasSelection() == true
        val hasConfiguredTerminal = if (project != null) {
            val terminalTitle = AiAgentCliBridgeSettings.getInstance().state.terminalTitle
            TerminalActionSupport.hasTerminalWithTitle(project, terminalTitle)
        } else {
            false
        }

        event.presentation.isEnabledAndVisible = project != null && hasSelection && hasConfiguredTerminal
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}