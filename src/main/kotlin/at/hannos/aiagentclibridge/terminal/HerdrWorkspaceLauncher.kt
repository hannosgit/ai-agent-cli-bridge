package at.hannos.aiagentclibridge.terminal

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.execution.ParametersListUtil
import org.jetbrains.plugins.terminal.TerminalProjectOptionsProvider

object HerdrWorkspaceLauncher {
    private val logger = Logger.getInstance(HerdrWorkspaceLauncher::class.java)

    fun prepareCommand(project: Project, command: String): String {
        if (command.trim() != "herdr") return command
        val selection = selectWorkspace(project) ?: return command
        return "herdr workspace focus ${quote(selection.id)} >/dev/null; $command"
    }

    fun focusWorkspace(project: Project, shouldFocus: () -> Boolean) {
        if (!shouldFocus()) return
        val selection = selectWorkspace(project) ?: return
        if (!shouldFocus()) return
        try {
            CapturingProcessHandler(selection.focusCommand).runProcess(5000)
        } catch (exception: ProcessCanceledException) {
            throw exception
        } catch (exception: Exception) {
            logger.debug("Could not focus the Herdr workspace for the project", exception)
        }
    }

    private data class WorkspaceSelection(val id: String, val focusCommand: GeneralCommandLine)

    private fun selectWorkspace(project: Project): WorkspaceSelection? {
        val projectPath = project.basePath ?: return null
        return try {
            val options = TerminalProjectOptionsProvider.getInstance(project)
            val environment = options.getEnvData().envs
            val query = queryCommand(options.shellPath, projectPath, SystemInfo.isWindows, environment)
                ?: return null
            val commandLine = GeneralCommandLine(query)
                .withEnvironment(environment)
                .withCharset(Charsets.UTF_8)
            val output = CapturingProcessHandler(commandLine).runProcess(5000)
            if (output.isTimeout || output.exitCode != 0) return null
            val payload = output.stdout.substringAfterLast('\u001e', "")
            val path = payload.substringBefore('\n').trimEnd('\r')
            if (path.isBlank()) return null
            val workspace = findWorkspace(path, payload.substringAfter('\n', "")) ?: return null
            val focus = queryCommand(
                options.shellPath, projectPath, SystemInfo.isWindows, environment,
                "herdr workspace focus ${quote(workspace)}",
            ) ?: return null
            WorkspaceSelection(workspace, GeneralCommandLine(focus).withEnvironment(environment).withCharset(Charsets.UTF_8))
        } catch (exception: ProcessCanceledException) {
            throw exception
        } catch (exception: Exception) {
            logger.debug("Could not select a Herdr workspace for the project", exception)
            null
        }
    }

    internal fun queryCommand(
        shellPath: String,
        projectPath: String,
        windows: Boolean,
        environment: Map<String, String> = emptyMap(),
        operation: String = "printf '\\036%s\\n' \"\$(pwd -P)\" && herdr api snapshot",
    ): List<String>? {
        val shell = ParametersListUtil.parse(shellPath)
        val executable = shell.firstOrNull() ?: return null
        val name = executable.replace('\\', '/').substringAfterLast('/').lowercase()
        val prefix = when {
            name == "wsl.exe" || name == "wsl" -> {
                val arguments = mutableListOf(executable)
                var index = 1
                while (index < shell.size) {
                    val option = shell[index++]
                    if (option !in listOf("-d", "--distribution", "-u", "--user", "--cd")) return null
                    val value = shell.getOrNull(index++) ?: return null
                    if (option != "--cd") arguments.addAll(listOf(option, value))
                }
                arguments + listOf("--exec", "env") + environment.map { (key, value) -> "$key=$value" } +
                    listOf("bash", "-lic")
            }
            !windows && name in listOf("bash", "zsh") -> listOf(executable, "-lic")
            else -> return null
        }
        val path = if (windows) "\"\$(wslpath -a -u ${quote(projectPath)})\"" else quote(projectPath)
        return prefix + "cd -- $path && $operation"
    }

    internal fun findWorkspace(projectPath: String, response: String): String? {
        val snapshot = JsonParser.parseString(response).asJsonObject
            .getAsJsonObject("result")?.getAsJsonObject("snapshot") ?: return null
        val workspaces = snapshot.getAsJsonArray("workspaces") ?: return null
        val project = normalize(projectPath)
        val candidates = mutableMapOf<String, Int>()
        for (element in workspaces) {
            val workspace = element.asJsonObject
            val id = workspace.string("workspace_id") ?: continue
            val checkout = workspace.get("worktree")?.takeIf { it.isJsonObject }
                ?.asJsonObject?.string("checkout_path") ?: continue
            val path = normalize(checkout)
            if (contains(path, project)) candidates[id] = path.length
        }
        val ids = workspaces.mapNotNull { it.asJsonObject.string("workspace_id") }.toSet()
        for (element in snapshot.getAsJsonArray("panes") ?: return bestMatch(candidates)) {
            val pane = element.asJsonObject
            val id = pane.string("workspace_id") ?: continue
            if (id !in ids) continue
            for (field in listOf("cwd", "foreground_cwd")) {
                val path = pane.string(field)?.let(::normalize) ?: continue
                val score = when {
                    contains(path, project) -> path.length
                    contains(project, path) -> project.length
                    else -> continue
                }
                candidates[id] = maxOf(candidates[id] ?: 0, score)
            }
        }
        return bestMatch(candidates)
    }

    private fun bestMatch(candidates: Map<String, Int>): String? {
        val score = candidates.values.maxOrNull() ?: return null
        return candidates.filterValues { it == score }.keys.singleOrNull()
    }

    private fun JsonObject.string(name: String): String? =
        get(name)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString?.takeIf { it.isNotBlank() }

    private fun normalize(path: String): String = path.replace('\\', '/').trimEnd('/').ifEmpty { "/" }

    private fun contains(parent: String, child: String): Boolean =
        child == parent || child.startsWith(if (parent == "/") parent else "$parent/")

    private fun quote(value: String): String = "'" + value.replace("'", "'\"'\"'") + "'"
}
