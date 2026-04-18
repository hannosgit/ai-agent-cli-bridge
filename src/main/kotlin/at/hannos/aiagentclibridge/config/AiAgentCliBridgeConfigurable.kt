package at.hannos.aiagentclibridge.config

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class AiAgentCliBridgeConfigurable : Configurable {
    private var component: JPanel? = null
    private var terminalTitleField: JBTextField? = null
    private var aiToolCommandField: JBTextField? = null
    private var aiReviewCommandField: JBTextField? = null
    private var dynamicActionsPanel: JPanel? = null
    private val dynamicActionRows = mutableListOf<DynamicActionRow>()

    private data class DynamicActionRow(
        val container: JPanel,
        val actionTextField: JBTextField,
        val promptField: JBTextField,
    )

    override fun getDisplayName(): String = "AI Agent CLI Bridge"

    override fun createComponent(): JComponent {
        val terminalField = JBTextField()
        val launchProgramField = JBTextField()
        val reviewCommandField = JBTextField()
        val actionsContainer = JPanel()
        actionsContainer.layout = BoxLayout(actionsContainer, BoxLayout.Y_AXIS)

        val addDynamicActionButton = JButton("Add dynamic action")
        addDynamicActionButton.addActionListener {
            addDynamicActionRow()
            refreshDynamicActionsPanel()
        }

        val dynamicActionsHeader = JPanel(GridBagLayout())
        val headerConstraints = GridBagConstraints().apply {
            gridy = 0
            insets = Insets(0, 0, 4, 4)
            anchor = GridBagConstraints.WEST
            fill = GridBagConstraints.HORIZONTAL
        }
        headerConstraints.gridx = 0
        headerConstraints.weightx = 0.4
        dynamicActionsHeader.add(JLabel("Action text"), headerConstraints)
        headerConstraints.gridx = 1
        headerConstraints.weightx = 0.6
        dynamicActionsHeader.add(JLabel("Prompt"), headerConstraints)

        val dynamicActionsWrapper = JBPanel<JBPanel<*>>(BorderLayout())
        dynamicActionsWrapper.add(dynamicActionsHeader, BorderLayout.NORTH)
        dynamicActionsWrapper.add(actionsContainer, BorderLayout.CENTER)
        dynamicActionsWrapper.add(addDynamicActionButton, BorderLayout.SOUTH)

        terminalTitleField = terminalField
        aiToolCommandField = launchProgramField
        aiReviewCommandField = reviewCommandField
        dynamicActionsPanel = actionsContainer
        val panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Terminal title:", terminalField)
            .addLabeledComponent("Launch AI tool command:", launchProgramField)
            .addLabeledComponent("AI review command:", reviewCommandField)
            .addLabeledComponent("Dynamic actions:", dynamicActionsWrapper)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        component = panel
        return panel
    }

    override fun isModified(): Boolean {
        val settings = AiAgentCliBridgeSettings.getInstance()
        val state = settings.state
        val stateDynamicActions = settings.getEffectiveDynamicActions(state.dynamicActions)
        val currentDynamicActions = dynamicActionRows.map {
            AiAgentCliBridgeSettings.DynamicAction(
                actionText = it.actionTextField.text,
                prompt = it.promptField.text,
            )
        }
        return terminalTitleField?.text != state.terminalTitle ||
            aiToolCommandField?.text != state.launchProgramWhenNoTerminalFound ||
            aiReviewCommandField?.text != state.aiReviewCommand ||
            currentDynamicActions != stateDynamicActions
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        val state = AiAgentCliBridgeSettings.getInstance().state
        state.terminalTitle = terminalTitleField?.text ?: ""
        state.launchProgramWhenNoTerminalFound = aiToolCommandField?.text ?: ""
        state.aiReviewCommand = aiReviewCommandField?.text ?: ""
        state.dynamicActions = dynamicActionRows.map {
            AiAgentCliBridgeSettings.DynamicAction(
                actionText = it.actionTextField.text,
                prompt = it.promptField.text,
            )
        }.toMutableList()
    }

    override fun reset() {
        val settings = AiAgentCliBridgeSettings.getInstance()
        val state = settings.state
        terminalTitleField?.text = state.terminalTitle
        aiToolCommandField?.text = state.launchProgramWhenNoTerminalFound
        aiReviewCommandField?.text = state.aiReviewCommand
        dynamicActionRows.clear()
        settings.getEffectiveDynamicActions(state.dynamicActions).forEach {
            addDynamicActionRow(it.actionText, it.prompt)
        }
        refreshDynamicActionsPanel()
    }

    override fun disposeUIResources() {
        component = null
        terminalTitleField = null
        aiToolCommandField = null
        aiReviewCommandField = null
        dynamicActionsPanel = null
        dynamicActionRows.clear()
    }

    private fun addDynamicActionRow(actionText: String = "", prompt: String = "") {
        val actionTextField = JBTextField(actionText)
        val promptField = JBTextField(prompt)
        actionTextField.columns = 20
        promptField.columns = 40
        val removeButton = JButton("Remove")

        val rowPanel = JPanel(GridBagLayout())
        val constraints = GridBagConstraints().apply {
            insets = Insets(0, 0, 4, 4)
            fill = GridBagConstraints.HORIZONTAL
            weighty = 0.0
        }

        constraints.gridx = 0
        constraints.gridy = 0
        constraints.weightx = 0.4
        rowPanel.add(actionTextField, constraints)

        constraints.gridx = 1
        constraints.weightx = 0.6
        rowPanel.add(promptField, constraints)

        constraints.gridx = 2
        constraints.gridy = 0
        constraints.weightx = 0.0
        constraints.fill = GridBagConstraints.NONE
        constraints.insets = Insets(0, 0, 4, 0)
        rowPanel.add(removeButton, constraints)

        val row = DynamicActionRow(
            container = rowPanel,
            actionTextField = actionTextField,
            promptField = promptField,
        )
        removeButton.addActionListener {
            dynamicActionRows.remove(row)
            refreshDynamicActionsPanel()
        }
        dynamicActionRows.add(row)
    }

    private fun refreshDynamicActionsPanel() {
        val panel = dynamicActionsPanel ?: return
        panel.removeAll()
        dynamicActionRows.forEach { panel.add(it.container) }
        panel.revalidate()
        panel.repaint()
    }

}