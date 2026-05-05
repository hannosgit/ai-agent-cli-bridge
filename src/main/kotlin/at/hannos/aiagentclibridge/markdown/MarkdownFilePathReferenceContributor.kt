package at.hannos.aiagentclibridge.markdown

import at.hannos.aiagentclibridge.console.FilePathLinkSupport
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.impl.FakePsiElement
import com.intellij.util.ProcessingContext
import org.intellij.plugins.markdown.lang.MarkdownLanguage

class MarkdownFilePathReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement().withLanguage(MarkdownLanguage.INSTANCE),
            MarkdownFilePathReferenceProvider,
        )
    }
}

internal object MarkdownFilePathReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        if (!element.isMarkdownLeaf()) return PsiReference.EMPTY_ARRAY

        return FilePathLinkSupport.findAll(element.project, element.text)
            .map {
                MarkdownFilePathReference(
                    element,
                    it.range.toTextRange(),
                    findLineMatch(element, it) ?: it,
                )
            }
            .toTypedArray()
    }

    fun PsiElement.isMarkdownLeaf(): Boolean {
        if (firstChild != null) return false

        val virtualFile = containingFile?.virtualFile ?: return false
        if (!virtualFile.extension.equals("md", ignoreCase = true)) return false

        return language.isKindOf(MarkdownLanguage.INSTANCE) || containingFile.language.isKindOf(MarkdownLanguage.INSTANCE)
    }

    private fun findLineMatch(element: PsiElement, elementMatch: FilePathLinkSupport.Match): FilePathLinkSupport.Match? {
        val document = element.containingFile.fileDocument
        val lineNumber = document.getLineNumber(element.textRange.startOffset + elementMatch.range.first)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val lineText = document.getText(TextRange(lineStartOffset, document.getLineEndOffset(lineNumber)))
        val matchStartInLine = element.textRange.startOffset - lineStartOffset + elementMatch.range.first

        return FilePathLinkSupport.findAll(element.project, lineText)
            .firstOrNull { it.range.first == matchStartInLine }
    }

    fun findMatchAtOffset(element: PsiElement, offset: Int): FilePathLinkSupport.Match? {
        if (!element.isMarkdownLeaf()) return null

        val document = element.containingFile.fileDocument
        val lineNumber = document.getLineNumber(offset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val lineText = document.getText(TextRange(lineStartOffset, document.getLineEndOffset(lineNumber)))
        val offsetInLine = offset - lineStartOffset

        return FilePathLinkSupport.findAll(element.project, lineText)
            .firstOrNull { offsetInLine in it.range }
    }

    private fun IntRange.toTextRange(): TextRange = TextRange(first, last + 1)

}

class MarkdownFilePathGotoDeclarationHandler : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(sourceElement: PsiElement?, offset: Int, editor: Editor): Array<PsiElement>? {
        val element = sourceElement ?: return null
        val match = MarkdownFilePathReferenceProvider.findMatchAtOffset(element, offset) ?: return null
        val target = PsiManager.getInstance(element.project).findFile(match.virtualFile) ?: return null

        return arrayOf(MarkdownFilePathNavigationTarget(target, match))
    }
}

private class MarkdownFilePathNavigationTarget(
    private val target: PsiElement,
    private val match: FilePathLinkSupport.Match,
) : FakePsiElement(), Navigatable {

    override fun getParent(): PsiElement = target

    override fun getContainingFile(): PsiFile? = target.containingFile

    override fun getProject() = target.project

    override fun getName(): String = match.text

    override fun navigate(requestFocus: Boolean) {
        val descriptor = OpenFileDescriptor(project, match.virtualFile, match.lineNumber, match.column)
        FileEditorManager.getInstance(project).openTextEditor(descriptor, requestFocus)
    }

    override fun canNavigate(): Boolean = true

    override fun canNavigateToSource(): Boolean = true
}

class MarkdownFilePathAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (!with(MarkdownFilePathReferenceProvider) { element.isMarkdownLeaf() }) return

        FilePathLinkSupport.findAll(element.project, element.text)
            .forEach {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                    .range(it.range.toTextRange().shiftRight(element.textRange.startOffset))
                    .textAttributes(CodeInsightColors.HYPERLINK_ATTRIBUTES)
                    .create()
            }
    }

    private fun IntRange.toTextRange(): TextRange = TextRange(first, last + 1)
}

private class MarkdownFilePathReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val match: FilePathLinkSupport.Match,
) : PsiReferenceBase<PsiElement>(element, rangeInElement, true), Navigatable, HighlightedReference {

    override fun resolve(): PsiElement? = PsiManager.getInstance(element.project).findFile(match.virtualFile)

    override fun getCanonicalText(): String = match.text

    override fun navigate(requestFocus: Boolean) {
        val descriptor = OpenFileDescriptor(element.project, match.virtualFile, match.lineNumber, match.column)
        FileEditorManager.getInstance(element.project).openTextEditor(descriptor, requestFocus)
    }

    override fun canNavigate(): Boolean = true

    override fun canNavigateToSource(): Boolean = true
}