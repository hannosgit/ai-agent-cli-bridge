package at.hannos.aiagentclibridge.console

import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.Filter.Result
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem

class FilePathFilter(private val project: Project) : Filter {

    // Matches Unix-style relative paths:    src/main/Foo.java[:42[:10]]
    // Matches Windows-style relative paths: src\main\Foo.java[:42[:10]]
    // Matches absolute Unix paths:          /home/user/Foo.java[:42[:10]]
    // Matches absolute Windows paths:       C:\Users\user\Foo.java[:42[:10]] or C:/Users/user/Foo.java[:42[:10]]
    private val pattern = Regex("""((?:[A-Za-z]:[\\/])?[\w.\-\\/]+\.\w+)(?::(\d+))?(?::(\d+))?""")

    override fun applyFilter(line: String, entireLength: Int): Result? {
        val match = pattern.find(line) ?: return null

        val rawPath = match.groupValues[1]
        val lineNumber = match.groupValues[2].toIntOrNull()?.minus(1) ?: 0
        val column = match.groupValues[3].toIntOrNull()?.minus(1) ?: 0

        // Normalize backslashes to forward slashes for VFS lookup
        val relativePath = rawPath.replace('\\', '/')

        val isAbsolute = relativePath.startsWith("/") ||
                relativePath.length >= 2 && relativePath[1] == ':'

        val absolutePath = if (isAbsolute) relativePath else "${project.basePath}/$relativePath"
        val virtualFile = LocalFileSystem.getInstance()
            .findFileByPath(absolutePath) ?: return null

        val startOffset = entireLength - line.length + match.range.first
        val endOffset = entireLength - line.length + match.range.last + 1

        val info = HyperlinkInfo { proj ->
            val descriptor = OpenFileDescriptor(proj, virtualFile, lineNumber, column)
            FileEditorManager.getInstance(proj).openTextEditor(descriptor, true)
        }

        return Result(startOffset, endOffset, info)
    }
}