package at.hannos.aiagentclibridge.action

import at.hannos.aiagentclibridge.MyIcons
import at.hannos.aiagentclibridge.config.AiAgentCliBridgeSettings
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class DynamicSelectionActionsGroup : ActionGroup() {

    init {
        templatePresentation.icon = MyIcons.MyIcon
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val settings = AiAgentCliBridgeSettings.getInstance()
        val dynamicActions = settings.getEffectiveDynamicActions(settings.state.dynamicActions)
        return dynamicActions
            .asSequence()
            .map { it.actionText.trim() to it.prompt.trim() }
            .filter { (actionText, prompt) -> actionText.isNotEmpty() && prompt.isNotEmpty() }
            .map { (actionText, prompt) -> DynamicSelectionToTerminalAction(actionText, prompt) }
            .toList()
            .toTypedArray()
    }

    override fun update(e: AnActionEvent) {
        val settings = AiAgentCliBridgeSettings.getInstance()
        val hasDynamicActions = settings.getEffectiveDynamicActions(settings.state.dynamicActions).any {
            it.actionText.isNotBlank() && it.prompt.isNotBlank()
        }
        e.presentation.isVisible = hasDynamicActions
        e.presentation.isEnabled = hasDynamicActions
    }
}
