package at.hannos.aiagentclibridge.console

import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.Filter.Result
import com.intellij.openapi.project.Project

class FilePathFilter(private val project: Project) : Filter {

    override fun applyFilter(line: String, entireLength: Int): Result? {
        val match = FilePathLinkSupport.findFirst(project, line) ?: return null

        val startOffset = entireLength - line.length + match.range.first
        val endOffset = entireLength - line.length + match.range.last + 1

        return Result(startOffset, endOffset, FilePathLinkSupport.createHyperlinkInfo(match))
    }
}