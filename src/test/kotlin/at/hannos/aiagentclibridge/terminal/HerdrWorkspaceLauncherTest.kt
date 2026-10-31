package at.hannos.aiagentclibridge.terminal

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import junit.framework.TestCase

class HerdrWorkspaceLauncherTest : TestCase() {
    fun testMatchesRenamedWorkspaceByPaneDirectory() {
        assertEquals("w2", match("/projects/api", listOf("w1" to "/projects/web", "w2" to "/projects/api/src")))
    }

    fun testDoesNotMatchDirectoryNamePrefix() {
        assertNull(match("/projects/api", listOf("w1" to "/projects/api-old")))
    }

    fun testChoosesMostSpecificWorkspaceForNestedProject() {
        assertEquals("w2", match("/projects/api/module", listOf("w1" to "/projects", "w2" to "/projects/api")))
    }

    fun testSkipsAmbiguousWorkspaces() {
        assertNull(match("/projects/api", listOf("w1" to "/projects/api", "w2" to "/projects/api/src")))
    }

    fun testMultiplePanesInOneWorkspaceAreNotAmbiguous() {
        assertEquals("w1", match("/projects/api/", listOf("w1" to "/projects/api", "w1" to "/projects/api/src")))
    }

    fun testMatchesCheckoutEvenWhenPaneHasChangedDirectory() {
        val snapshot = snapshot(listOf("w1" to "/tmp"))
        snapshot.getAsJsonArray("workspaces")[0].asJsonObject.add("worktree", JsonObject().apply {
            addProperty("checkout_path", "/projects/api")
        })
        assertEquals("w1", HerdrWorkspaceLauncher.findWorkspace("/projects/api", response(snapshot)))
    }

    fun testUsesForegroundDirectory() {
        val snapshot = snapshot(listOf("w1" to "/tmp"))
        snapshot.getAsJsonArray("panes")[0].asJsonObject.addProperty("foreground_cwd", "/projects/api")
        assertEquals("w1", HerdrWorkspaceLauncher.findWorkspace("/projects/api", response(snapshot)))
    }

    fun testErrorResponseDoesNotMatch() {
        assertNull(HerdrWorkspaceLauncher.findWorkspace("/projects/api", """{"error":{"message":"unavailable"}}"""))
    }

    fun testWslQueryPreservesDistributionAndUser() {
        val command = HerdrWorkspaceLauncher.queryCommand(
            "wsl.exe --distribution Ubuntu -u hannes", "D:\\My Project\\api", true
        )!!
        assertEquals(
            listOf("wsl.exe", "--distribution", "Ubuntu", "-u", "hannes", "--exec", "env", "bash", "-lic"),
            command.dropLast(1),
        )
        assertTrue(command.last().contains("wslpath -a -u 'D:\\My Project\\api'"))
    }

    fun testWslQueryUsesTerminalEnvironmentAndProjectDirectory() {
        val command = HerdrWorkspaceLauncher.queryCommand(
            "wsl.exe -d Ubuntu --cd ~", "D:/project", true,
            mapOf("HERDR_CONFIG_PATH" to "/home/hannes/my config.toml"),
        )!!
        assertEquals(
            listOf("wsl.exe", "-d", "Ubuntu", "--exec", "env", "HERDR_CONFIG_PATH=/home/hannes/my config.toml", "bash", "-lic"),
            command.dropLast(1),
        )
        assertTrue(command.last().startsWith("cd -- "))
    }

    fun testUnixQueryQuotesShellMetacharacters() {
        val command = HerdrWorkspaceLauncher.queryCommand("/bin/bash", "/projects/Bob's \$(touch unsafe)", false)!!
        assertTrue(command.last().contains("'/projects/Bob'\"'\"'s \$(touch unsafe)'"))
    }

    fun testFocusRunsThroughSameWslDistributionInProjectDirectory() {
        val command = HerdrWorkspaceLauncher.queryCommand(
            "wsl.exe --distribution Debian", "D:/project", true,
            operation = "herdr workspace focus 'w2'",
        )!!
        assertEquals(listOf("wsl.exe", "--distribution", "Debian", "--exec", "env", "bash", "-lic"), command.dropLast(1))
        assertEquals("cd -- \"\$(wslpath -a -u 'D:/project')\" && herdr workspace focus 'w2'", command.last())
    }

    fun testUnsupportedShellsAreSkipped() {
        assertNull(HerdrWorkspaceLauncher.queryCommand("powershell.exe", "D:/project", true))
        assertNull(HerdrWorkspaceLauncher.queryCommand("wsl.exe --exec fish", "D:/project", true))
    }

    private fun match(project: String, panes: List<Pair<String, String>>): String? =
        HerdrWorkspaceLauncher.findWorkspace(project, response(snapshot(panes)))

    private fun response(snapshot: JsonObject): String = JsonObject().apply {
        add("result", JsonObject().apply { add("snapshot", snapshot) })
    }.toString()

    private fun snapshot(panes: List<Pair<String, String>>): JsonObject = JsonObject().apply {
        add("workspaces", JsonArray().apply {
            panes.map { it.first }.distinct().forEach { id ->
                add(JsonObject().apply {
                    addProperty("workspace_id", id)
                    addProperty("label", "Custom label")
                })
            }
        })
        add("panes", JsonArray().apply {
            panes.forEach { (id, cwd) ->
                add(JsonObject().apply {
                    addProperty("workspace_id", id)
                    addProperty("cwd", cwd)
                })
            }
        })
    }
}
