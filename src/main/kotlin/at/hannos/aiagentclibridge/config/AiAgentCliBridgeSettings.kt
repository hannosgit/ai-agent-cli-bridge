package at.hannos.aiagentclibridge.config

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@Service(Service.Level.APP)
@State(name = "AiAgentCliBridgeSettings", storages = [Storage("ai-agent-cli-bridge.xml")])
class AiAgentCliBridgeSettings : PersistentStateComponent<AiAgentCliBridgeSettings.State> {

    data class State(
        var terminalTitle: String = "AI CLI Tool",
        var launchProgramWhenNoTerminalFound: String = "claude",
        var aiReviewCommand: String = "/review",
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(): AiAgentCliBridgeSettings = service()
    }
}