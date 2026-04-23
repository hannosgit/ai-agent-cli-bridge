package at.hannos.aiagentclibridge.console

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

class FilePathFilterTest : BasePlatformTestCase() {

    private lateinit var filter: FilePathFilter
    private lateinit var baseDir: File

    override fun setUp() {
        super.setUp()
        filter = FilePathFilter(project)
        baseDir = File(project.basePath!!)
        baseDir.mkdirs()
    }

    private fun createProjectFile(relativePath: String): File {
        val file = File(baseDir, relativePath)
        file.parentFile.mkdirs()
        file.writeText("// test file\n")
        runWriteAction {
            VfsUtil.markDirtyAndRefresh(
                false,
                true,
                true,
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
            )
        }
        return file
    }

    fun testMatchesRelativeUnixPath() {
        val rel = "src/main/Foo.java"
        createProjectFile(rel)
        val line = "error at $rel something"
        val result = filter.applyFilter(line, line.length)
        assertNotNull(result)
        val item = result!!.resultItems.single()
        assertEquals(line.indexOf(rel), item.highlightStartOffset)
        assertEquals(line.indexOf(rel) + rel.length, item.highlightEndOffset)
    }

    fun testMatchesRelativeWindowsPath() {
        createProjectFile("src/main/Bar.kt")
        val line = """prefix src\main\Bar.kt suffix"""
        val result = filter.applyFilter(line, line.length)
        assertNotNull(result)
        val item = result!!.resultItems.single()
        val expected = """src\main\Bar.kt"""
        assertEquals(line.indexOf(expected), item.highlightStartOffset)
        assertEquals(line.indexOf(expected) + expected.length, item.highlightEndOffset)
    }

    fun testMatchesWithLineNumber() {
        val rel = "src/main/Baz.kt"
        createProjectFile(rel)
        val line = "see $rel:42 now"
        val result = filter.applyFilter(line, line.length)
        assertNotNull(result)
        val item = result!!.resultItems.single()
        val full = "$rel:42"
        assertEquals(line.indexOf(full), item.highlightStartOffset)
        assertEquals(line.indexOf(full) + full.length, item.highlightEndOffset)
    }

    fun testMatchesWithLineAndColumn() {
        val rel = "src/main/Qux.kt"
        createProjectFile(rel)
        val line = "$rel:10:5"
        val result = filter.applyFilter(line, line.length)
        assertNotNull(result)
        val item = result!!.resultItems.single()
        assertEquals(0, item.highlightStartOffset)
        assertEquals(line.length, item.highlightEndOffset)
    }

    fun testOffsetWithEntireLengthGreaterThanLine() {
        val rel = "src/main/Off.kt"
        createProjectFile(rel)
        val line = "at $rel end"
        val entireLength = line.length + 100
        val result = filter.applyFilter(line, entireLength)
        assertNotNull(result)
        val item = result!!.resultItems.single()
        val startInLine = line.indexOf(rel)
        assertEquals(entireLength - line.length + startInLine, item.highlightStartOffset)
        assertEquals(entireLength - line.length + startInLine + rel.length, item.highlightEndOffset)
    }

    fun testReturnsNullForNonExistingFile() {
        val line = "ref does/not/exist/Nope.kt:1:1"
        val result = filter.applyFilter(line, line.length)
        assertNull(result)
    }

    fun testReturnsNullForLineWithoutPath() {
        val line = "just a plain log message without any path"
        val result = filter.applyFilter(line, line.length)
        assertNull(result)
    }

    fun testMatchesAbsolutePath() {
        val rel = "src/main/Abs.kt"
        val file = createProjectFile(rel)
        val absolute = file.absolutePath.replace('\\', '/')
        val line = "see $absolute:7"
        val result = filter.applyFilter(line, line.length)
        assertNotNull(result)
        val item = result!!.resultItems.single()
        val full = "$absolute:7"
        assertEquals(line.indexOf(full), item.highlightStartOffset)
        assertEquals(line.indexOf(full) + full.length, item.highlightEndOffset)
    }

    fun testPathWithHyphenAndDot() {
        val rel = "src/main/my-module/file.name.kt"
        createProjectFile(rel)
        val line = "at $rel here"
        val result = filter.applyFilter(line, line.length)
        assertNotNull(result)
    }

    fun testMatchesNextJsRouteGroupParentheses() {
        val rel = "src/app/(loggedin)/page.tsx"
        createProjectFile(rel)
        val line = "see $rel here"
        val result = filter.applyFilter(line, line.length)
        assertNotNull(result)
        val item = result!!.resultItems.single()
        assertEquals(line.indexOf(rel), item.highlightStartOffset)
        assertEquals(line.indexOf(rel) + rel.length, item.highlightEndOffset)
    }
}
