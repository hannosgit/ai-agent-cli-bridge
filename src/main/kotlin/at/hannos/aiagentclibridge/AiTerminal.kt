package at.hannos.aiagentclibridge

interface AiTerminal {

    fun sendText(text: String): Boolean

    fun sendTextAndExecute(text: String)

}