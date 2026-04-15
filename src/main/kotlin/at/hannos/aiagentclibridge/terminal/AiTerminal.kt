package at.hannos.aiagentclibridge.terminal

interface AiTerminal {

    fun sendText(text: String): Boolean

    fun sendTextAndExecute(text: String)

}