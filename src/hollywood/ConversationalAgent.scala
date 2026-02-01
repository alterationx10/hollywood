package hollywood

import hollywood.clients.completions.{
  ChatCompletionClient,
  ChatMessage
}
import hollywood.tools.ToolRegistry

class ConversationalAgent(
    completionClient: ChatCompletionClient = ChatCompletionClient(),
    toolRegistry: Option[ToolRegistry] = None,
    maxTurns: Int = 50,
    model: String,
    onTurn: Option[(Int, ChatMessage) => Unit] = None,
    conversationState: ConversationState = new InMemoryState()
) extends Agent {

  def chat(message: String): String = {
    // Add user message to conversation
    val userMessage     = ChatMessage(role = "user", content = Some(message))
    val currentMessages = conversationState.get :+ userMessage

    // Run shared conversation loop
    val (response, updatedMessages) = AgentConversationLoop.run(
      currentMessages,
      completionClient,
      toolRegistry,
      maxTurns,
      model,
      onTurn
    )
    conversationState.update(updatedMessages)

    response
  }
}
