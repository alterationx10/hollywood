package hollywood

import ujson.Value
import hollywood.tools.schema.{Schema, ToolSchema}
import hollywood.tools.{AgentChatTool, CallableTool, ToolExecutor}

import scala.util.*

trait Agent {
  def chat(message: String): String
}

object Agent {

  // Protected implementation to avoid anonymous class duplication at inline sites
  protected class AgentToolExecutor(agent: Agent)
      extends ToolExecutor[AgentChatTool] {
    override def execute(
        args: Value
    ): Value = {
      val message = args match {
        case ujson.Obj(obj) =>
          obj.get("message") match {
            case Some(ujson.Str(str)) => str
            case _                    => "No message provided"
          }
        case _              => "Invalid arguments"
      }
      val result  = Try(agent.chat(message)) match {
        case Success(value) => ujson.Str(value)
        case Failure(e)     =>
          ujson.Str(s"Error executing Agent tool: ${e.getMessage}")
      }
      result
    }
  }

  /** Derives a tool that wraps an agent's chat method for agent-to-agent
    * communication
    *
    * @param agent
    *   The agent instance to wrap
    * @param agentName
    *   Name to use for the tool (defaults to agent's class name)
    * @param description
    *   Description of what the agent does
    * @return
    *   Tuple of (ToolSchema, ToolExecutor) ready to register in a ToolRegistry
    */
  inline def deriveAgentTool[A <: Agent](
      agent: A,
      agentName: Option[String] = None,
      description: String = "Chat with an agent to get specialized assistance"
  ): (ToolSchema, ToolExecutor[? <: CallableTool[?]]) = {

    val toolName =
      agentName.getOrElse(agent.getClass.getSimpleName.stripSuffix("$"))

    // Create the schema manually since we're not using @Tool annotation
    val toolSchema = ToolSchema(
      name = toolName,
      description = description,
      parameters = Schema.ObjectSchema(
        properties = Map(
          "message" -> Schema.StringSchema(
            description = Some("The message to send to the agent")
          )
        ),
        required = List("message"),
        description = None
      )
    )

    // Create the executor using the private class
    val executor = new AgentToolExecutor(agent)

    (toolSchema, executor)
  }

}
