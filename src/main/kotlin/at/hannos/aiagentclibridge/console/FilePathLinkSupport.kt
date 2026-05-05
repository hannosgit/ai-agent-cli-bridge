package at.hannos.aiagentclibridge.console

import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile

object FilePathLinkSupport {

    // Matches Unix-style relative paths:    src/main/Foo.java[:42[:10]]
    // Matches Windows-style relative paths: src\main\Foo.java[:42[:10]]
    // Matches absolute Unix paths:          /home/user/Foo.java[:42[:10]]
    // Matches absolute Windows paths:       C:\Users\user\Foo.java[:42[:10]] or C:/Users/user/Foo.java[:42[:10]]
    // Also supports path segments in square brackets (e.g. Next.js dynamic routes): src/main/[id]/Foo.java
    // Also supports path segments in parentheses (e.g. Next.js route groups): src/app/(loggedin)/page.tsx
    private val pattern = Regex("""((?:[A-Za-z]:[\\/])?[\w.\-\\/\[\]()]+\.\w+)(?::(\d+))?(?::(\d+))?""")

    data class Match(
        val range: IntRange,
        val text: String,
        val virtualFile: VirtualFile,
        val lineNumber: Int,
        val column: Int,
    )

    fun findFirst(project: Project, text: String): Match? = findAll(project, text).firstOrNull()

    fun findAll(project: Project, text: String): List<Match> = pattern.findAll(text)
        .mapNotNull { it.toFilePathMatch(project) }
        .toList()

    fun createHyperlinkInfo(match: Match): HyperlinkInfo = HyperlinkInfo { project ->
        val descriptor = OpenFileDescriptor(project, match.virtualFile, match.lineNumber, match.column)
        FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
    }

    private fun MatchResult.toFilePathMatch(project: Project): Match? {
        val rawPath = groupValues[1]
        val lineNumber = groupValues[2].toIntOrNull()?.minus(1) ?: 0
        val column = groupValues[3].toIntOrNull()?.minus(1) ?: 0

        val relativePath = rawPath.replace('\\', '/')
        val isAbsolute = relativePath.startsWith("/") ||
                relativePath.length >= 2 && relativePath[1] == ':'

        val absolutePath = if (isAbsolute) relativePath else "${project.basePath}/$relativePath"
        val virtualFile = LocalFileSystem.getInstance()
            .findFileByPath(absolutePath) ?: return null

        return Match(range, value, virtualFile, lineNumber, column)
    }
}