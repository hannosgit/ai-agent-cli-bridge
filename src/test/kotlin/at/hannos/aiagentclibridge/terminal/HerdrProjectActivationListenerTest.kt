package at.hannos.aiagentclibridge.terminal

import at.hannos.aiagentclibridge.config.AiAgentCliBridgeSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFrame
import com.intellij.openapi.wm.StatusBar
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.BalloonLayout
import java.awt.Rectangle
import javax.swing.JPanel

class HerdrProjectActivationListenerTest : BasePlatformTestCase() {
    private lateinit var originalCommand: String
    private val tasks = mutableListOf<() -> Unit>()
    private val focusedProjects = mutableListOf<Project>()

    override fun setUp() {
        super.setUp()
        val state = AiAgentCliBridgeSettings.getInstance().state
        originalCommand = state.launchProgramWhenNoTerminalFound
        state.launchProgramWhenNoTerminalFound = "herdr"
    }

    override fun tearDown() {
        try {
            AiAgentCliBridgeSettings.getInstance().state.launchProgramWhenNoTerminalFound = originalCommand
        } finally {
            super.tearDown()
        }
    }

    fun testReturningToProjectFocusesWorkspaceInBackground() {
        val listener = listener()
        listener.applicationActivated(frame(project))
        assertTrue(focusedProjects.isEmpty())
        tasks.single().invoke()
        assertEquals(listOf(project), focusedProjects)
    }

    fun testNoTerminalDoesNotFocusOrLaunchHerdr() {
        listener(terminalExists = false).applicationActivated(frame(project))
        assertTrue(tasks.isEmpty())
    }

    fun testOtherLaunchCommandsAreUnaffected() {
        AiAgentCliBridgeSettings.getInstance().state.launchProgramWhenNoTerminalFound = "claude"
        listener().applicationActivated(frame(project))
        assertTrue(tasks.isEmpty())
    }

    fun testWelcomeWindowDoesNotFocusHerdr() {
        listener().applicationActivated(frame(null))
        assertTrue(tasks.isEmpty())
    }

    fun testSwitchingAwayDiscardsPendingFocus() {
        val listener = listener()
        val frame = frame(project)
        listener.applicationActivated(frame)
        listener.applicationDeactivated(frame)
        tasks.single().invoke()
        assertTrue(focusedProjects.isEmpty())
    }

    fun testOnlyLatestActivationCanFocusWorkspace() {
        val listener = listener()
        val frame = frame(project)
        listener.applicationActivated(frame)
        listener.applicationDeactivated(frame)
        listener.applicationActivated(frame)
        tasks.forEach { it() }
        assertEquals(listOf(project), focusedProjects)
    }

    fun testSwitchingAwayDuringLookupInvalidatesFocus() {
        var stillActive: (() -> Boolean)? = null
        val listener = HerdrProjectActivationListener({ true }, { tasks.add(it) }) { _, shouldFocus ->
            stillActive = shouldFocus
        }
        val frame = frame(project)
        listener.applicationActivated(frame)
        tasks.single().invoke()
        assertTrue(stillActive!!.invoke())
        listener.applicationDeactivated(frame)
        assertFalse(stillActive!!.invoke())
    }

    private fun listener(terminalExists: Boolean = true): HerdrProjectActivationListener =
        HerdrProjectActivationListener({ terminalExists }, { tasks.add(it) }) { project, shouldFocus ->
            if (shouldFocus()) focusedProjects.add(project)
        }

    private fun frame(project: Project?): IdeFrame = object : IdeFrame {
        override fun getProject(): Project? = project
        override fun getComponent(): JPanel = JPanel()
        override fun getStatusBar(): StatusBar = error("Unused")
        override fun suggestChildFrameBounds(): Rectangle = Rectangle()
        override fun getBalloonLayout(): BalloonLayout = error("Unused")
        override fun setFrameTitle(title: String) = Unit
    }
}
