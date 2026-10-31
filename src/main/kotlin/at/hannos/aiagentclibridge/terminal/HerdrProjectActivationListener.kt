package at.hannos.aiagentclibridge.terminal

import at.hannos.aiagentclibridge.action.TerminalActionSupport
import at.hannos.aiagentclibridge.config.AiAgentCliBridgeSettings
import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFrame
import java.util.concurrent.atomic.AtomicLong

class HerdrProjectActivationListener internal constructor(
    private val terminalExists: (Project) -> Boolean,
    private val enqueue: (() -> Unit) -> Unit,
    private val focusWorkspace: (Project, () -> Boolean) -> Unit,
) : ApplicationActivationListener {
    constructor() : this(
        { TerminalActionSupport.terminalIsFound(it) },
        { ApplicationManager.getApplication().executeOnPooledThread(Runnable(it)) },
        HerdrWorkspaceLauncher::focusWorkspace,
    )

    private val activation = AtomicLong()

    override fun applicationActivated(ideFrame: IdeFrame) {
        val request = activation.incrementAndGet()
        val project = ideFrame.project ?: return
        val command = AiAgentCliBridgeSettings.getInstance().state.launchProgramWhenNoTerminalFound
        if (project.isDisposed || command.trim() != "herdr") return
        if (!terminalExists(project)) return
        enqueue {
            val shouldFocus = { !project.isDisposed && activation.get() == request }
            if (shouldFocus()) focusWorkspace(project, shouldFocus)
        }
    }

    override fun applicationDeactivated(ideFrame: IdeFrame) {
        activation.incrementAndGet()
    }
}
