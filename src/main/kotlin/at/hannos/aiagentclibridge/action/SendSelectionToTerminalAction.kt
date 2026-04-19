package at.hannos.aiagentclibridge.action

import at.hannos.aiagentclibridge.MyIcons
import at.hannos.aiagentclibridge.action.TerminalActionSupport.buildReference
import at.hannos.aiagentclibridge.action.TerminalActionSupport.terminalIsFound
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory


open class SendSelectionToTerminalAction(text: String? = null) : AnAction(text) {

    init {
        templatePresentation.icon = MyIcons.MyIcon
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val selectionModel = editor.selectionModel

        if (virtualFile == null) {
            TerminalActionSupport.notifyError(project, "Failed to resolve selected file")
            return
        }

        if (!selectionModel.hasSelection()) {
            val reference = buildReference(null, null, project, virtualFile)
            val command = buildCommand(reference)
            TerminalActionSupport.sendToTerminal(project, command, executeCommand())
            return
        }

        val reference = buildReference(selectionModel, editor, project, virtualFile)
        val command = buildCommand(reference)

        TerminalActionSupport.sendToTerminal(project, command, executeCommand())
    }

    protected open fun getPromptText(): String? = null

    protected open fun executeCommand(): Boolean = false

    override fun update(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        val hasSelection = editor?.selectionModel?.hasSelection() == true
        if (!hasSelection) {
            return
        }

        event.presentation.isEnabledAndVisible = terminalIsFound(event)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    private fun buildCommand(reference: String): String {
        val promptText = getPromptText()?.trim().orEmpty()
        if (promptText.isEmpty()) {
            return reference
        }

        return "$promptText $reference"
    }
}