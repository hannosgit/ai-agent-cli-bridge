package at.hannos.aiagentclibridge.console

import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.Filter.Result
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem

class FilePathFilter(private val project: Project) : Filter {

    // Matches: src/main/Foo.java  or  src/main/Foo.java:42  or  src/main/Foo.java:42:10
    private val pattern = Regex("""([\w./\-]+\.\w+)(?::(\d+))?(?::(\d+))?""")

    override fun applyFilter(line: String, entireLength: Int): Result? {
        val match = pattern.find(line) ?: return null

        val relativePath = match.groupValues[1]
        val lineNumber = match.groupValues[2].toIntOrNull()?.minus(1) ?: 0
        val column = match.groupValues[3].toIntOrNull()?.minus(1) ?: 0

        val absolutePath = "${project.basePath}/$relativePath"
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