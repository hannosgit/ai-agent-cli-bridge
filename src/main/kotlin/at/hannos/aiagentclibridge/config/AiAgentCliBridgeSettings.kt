package at.hannos.aiagentclibridge.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
@State(name = "AiAgentCliBridgeSettings", storages = [Storage("ai-agent-cli-bridge.xml")])
class AiAgentCliBridgeSettings : PersistentStateComponent<AiAgentCliBridgeSettings.State> {

    data class DynamicAction(
        var actionText: String = "",
        var prompt: String = "",
    )

    data class State(
        var terminalTitle: String = "AI CLI Tool",
        var launchProgramWhenNoTerminalFound: String = "claude",
        var aiReviewCommand: String = "/review",
        var dynamicActions: MutableList<DynamicAction> = mutableListOf(),
    )

    val defaultDynamicActions: List<DynamicAction>
        get() = listOf(
            DynamicAction(actionText = "Generate Tests", prompt = "Generate tests"),
            DynamicAction(actionText = "Refactor", prompt = "Refactor"),
        )

    fun getEffectiveDynamicActions(dynamicActions: List<DynamicAction>): List<DynamicAction> = dynamicActions.ifEmpty {
        defaultDynamicActions
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(): AiAgentCliBridgeSettings = service()
    }
}