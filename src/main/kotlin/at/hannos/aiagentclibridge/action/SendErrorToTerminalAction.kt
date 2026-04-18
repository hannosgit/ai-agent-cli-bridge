package at.hannos.aiagentclibridge.action

import at.hannos.aiagentclibridge.MyIcons
import at.hannos.aiagentclibridge.action.TerminalActionSupport.buildLineReference
import at.hannos.aiagentclibridge.action.TerminalActionSupport.notifyError
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.util.IntentionName
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import javax.swing.Icon


class SendErrorToTerminalAction : IntentionAction, Iconable {

    override fun getText(): @IntentionName String {
        return "Send error to AI Tool"
    }

    override fun getFamilyName(): String {
        return "AI Agent CLI Bridge"
    }

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        psiFile: PsiFile?
    ): Boolean {
        val virtualFile = psiFile?.virtualFile
        return editor != null && isSupportedFile(virtualFile) && TerminalActionSupport.terminalIsFound(project)
    }

    override fun invoke(
        project: Project,
        editor: Editor?,
        psiFile: PsiFile?
    ) {

        val virtualFile = psiFile?.virtualFile
        val line = editor?.caretModel?.logicalPosition?.line?.plus(1)

        if (!isSupportedFile(virtualFile) || line == null) {
            notifyError(project, "Failed to resolve selected error location")
            return
        }

        val lineReference = buildLineReference(project, virtualFile ?: return, line)
        val command = "$lineReference fix this error"
        TerminalActionSupport.sendToTerminal(project, command, true)
    }

    override fun startInWriteAction(): Boolean {
        return false
    }

    private fun isSupportedFile(virtualFile: VirtualFile?): Boolean {
        return virtualFile != null && virtualFile.isValid && !virtualFile.isDirectory
    }

    override fun getIcon(flags: Int): Icon {
        return MyIcons.MyIcon
    }
}
