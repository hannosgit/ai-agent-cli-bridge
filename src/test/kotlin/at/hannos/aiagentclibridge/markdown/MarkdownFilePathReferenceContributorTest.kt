package at.hannos.aiagentclibridge.markdown

import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ProcessingContext
import java.io.File

class MarkdownFilePathReferenceContributorTest : BasePlatformTestCase() {

    private lateinit var baseDir: File

    override fun setUp() {
        super.setUp()
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

    fun testMarkdownRelativeFilePathReferenceResolves() {
        val rel = "src/main/Foo.kt"
        val targetFile = createProjectFile(rel)
        val text = "See $rel for details."

        val markdownFile = myFixture.addFileToProject("README.md", text)
        myFixture.configureFromExistingVirtualFile(markdownFile.virtualFile)

        val reference = findReferenceAt(text.indexOf(rel) + 1)

        assertNotNull(reference)
        assertEquals(
            LocalFileSystem.getInstance().findFileByIoFile(targetFile),
            reference!!.resolve()?.containingFile?.virtualFile,
        )
        assertTrue(reference is HighlightedReference)
    }

    fun testMarkdownReadmeJavaPathReferenceIsHighlighted() {
        val rel = "src/main/java/org/example/CustomDBOSClient.java"
        val targetFile = createProjectFile(rel)
        val text = "See $rel for details."

        val markdownFile = myFixture.addFileToProject("README.md", text)
        myFixture.configureFromExistingVirtualFile(markdownFile.virtualFile)

        val offset = text.indexOf(rel) + 1
        val reference = findReferenceAt(offset)
        val highlight = myFixture.doHighlighting().firstOrNull {
            it.forcedTextAttributesKey == CodeInsightColors.HYPERLINK_ATTRIBUTES &&
                    it.startOffset == text.indexOf(rel) &&
                    it.endOffset == text.indexOf(rel) + rel.length
        }

        assertNotNull(reference)
        assertEquals(
            LocalFileSystem.getInstance().findFileByIoFile(targetFile),
            reference!!.resolve()?.containingFile?.virtualFile,
        )
        assertNotNull(highlight)
    }

    fun testMarkdownReadmeJavaPathReferenceIsAvailableForCtrlClickNavigation() {
        val rel = "src/main/java/org/example/CustomDBOSClient.java"
        val targetFile = createProjectFile(rel)
        val text = "See ${rel.take(5)}<caret>${rel.drop(5)} for details."

        myFixture.configureByText("README.md", text)

        val sourceElement = myFixture.file.findElementAt(myFixture.caretOffset)
        val targets = MarkdownFilePathGotoDeclarationHandler()
            .getGotoDeclarationTargets(sourceElement, myFixture.caretOffset, myFixture.editor)

        assertNotNull(targets)
        assertEquals(
            LocalFileSystem.getInstance().findFileByIoFile(targetFile),
            targets!!.single().containingFile.virtualFile,
        )
    }

    fun testMarkdownFilePathReferenceIncludesLineAndColumnSuffix() {
        val rel = "src/main/Bar.kt"
        createProjectFile(rel)
        val fullReference = "$rel:12:3"
        val text = "See $fullReference for details."

        val markdownFile = myFixture.addFileToProject("README.md", text)
        myFixture.configureFromExistingVirtualFile(markdownFile.virtualFile)

        val reference = findReferenceAt(text.indexOf(rel) + 1)

        assertNotNull(reference)
        assertEquals(fullReference, reference!!.canonicalText)
    }

    private fun findReferenceAt(offset: Int) = generateSequence(myFixture.file.findElementAt(offset)) { it.parent }
        .flatMap { MarkdownFilePathReferenceProvider.getReferencesByElement(it, ProcessingContext()).asSequence() }
        .firstOrNull { it.rangeInElement.shiftRight(it.element.textRange.startOffset).containsOffset(offset) }
}