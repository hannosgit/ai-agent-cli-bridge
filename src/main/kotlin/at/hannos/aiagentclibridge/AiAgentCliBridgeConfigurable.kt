package at.hannos.aiagentclibridge

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class AiAgentCliBridgeConfigurable : Configurable {
    private var component: JPanel? = null
    private var terminalTitleField: JBTextField? = null
    private var launchProgramWhenNoTerminalFoundField: JBTextField? = null

    override fun getDisplayName(): String = "AI Agent CLI Bridge"

    override fun createComponent(): JComponent {
        val terminalField = JBTextField()
        val launchProgramField = JBTextField()
        terminalTitleField = terminalField
        launchProgramWhenNoTerminalFoundField = launchProgramField
        val panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Terminal title:", terminalField)
            .addLabeledComponent("Launch program when no terminal found:", launchProgramField)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        component = panel
        return panel
    }

    override fun isModified(): Boolean {
        val state = AiAgentCliBridgeSettings.getInstance().state
        return terminalTitleField?.text != state.terminalTitle ||
            launchProgramWhenNoTerminalFoundField?.text != state.launchProgramWhenNoTerminalFound
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        val state = AiAgentCliBridgeSettings.getInstance().state
        state.terminalTitle = terminalTitleField?.text ?: ""
        state.launchProgramWhenNoTerminalFound = launchProgramWhenNoTerminalFoundField?.text ?: ""
    }

    override fun reset() {
        val state = AiAgentCliBridgeSettings.getInstance().state
        terminalTitleField?.text = state.terminalTitle
        launchProgramWhenNoTerminalFoundField?.text = state.launchProgramWhenNoTerminalFound
    }

    override fun disposeUIResources() {
        component = null
        terminalTitleField = null
        launchProgramWhenNoTerminalFoundField = null
    }
}