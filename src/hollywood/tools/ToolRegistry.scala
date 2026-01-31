package hollywood.tools

import ujson.Value
import upickle.default.{Reader, Writer}
import hollywood.clients.completions.{
  FunctionDefinition,
  Tool
}
import hollywood.tools.schema.{ToolSchema, JsonSchema}
import hollywood.tools.{CallableTool, ToolExecutor}

import scala.collection.mutable
import scala.deriving.Mirror

trait ToolRegistry {
  def registerTool(
      schema: ToolSchema,
      executor: ToolExecutor[? <: CallableTool[?]]
  ): Unit
  def getSchemas: List[ToolSchema]
  def getFunctionDefinitions: List[FunctionDefinition]
  def getTools: List[Tool]
  def getSchemasJson: String
  def execute(toolName: String, args: Value): Option[Value]
  def clear(): Unit
  def getRegisteredToolNames: List[String]
}

object ToolRegistry {
  def apply(): ToolRegistry = MutableToolRegistry()

  // Extension method to add inline register to any ToolRegistry
  extension (registry: ToolRegistry) {
    inline def register[T <: CallableTool[?]](using
        m: Mirror.ProductOf[T],
        reader: Reader[T],
        writer: Writer[ToolExecutor.ResultType[T]]
    ): ToolRegistry = {
      val schema   = ToolSchema.derive[T]
      val executor = ToolExecutor.derived[T]
      registry.registerTool(schema, executor)
      registry
    }

    // Method to register with pre-built schema and executor (for agent tools)
    def register[A](
        schema: ToolSchema,
        executor: ToolExecutor[? <: CallableTool[A]]
    ): ToolRegistry = {
      registry.registerTool(schema, executor)
      registry
    }

    // Method to register a tuple of (ToolSchema, ToolExecutor) directly
    def register(
        tool: (ToolSchema, ToolExecutor[? <: CallableTool[?]])
    ): ToolRegistry = {
      registry.registerTool(tool._1, tool._2)
      registry
    }

    // Generic method to register any tool with a policy
    inline def registerWithPolicy[T <: CallableTool[?]](
        policy: ToolPolicy[T]
    )(using
        m: Mirror.ProductOf[T],
        writer: Writer[ToolExecutor.ResultType[T]],
        reader: Reader[T]
    ): ToolRegistry = {
      val schema             = ToolSchema.derive[T]
      val baseExecutor       = ToolExecutor.derived[T]
      val restrictedExecutor = new RestrictedExecutor[T](baseExecutor, policy)

      registry.registerTool(schema, restrictedExecutor)
      registry
    }

  }
}

case class MutableToolRegistry() extends ToolRegistry {
  private val tools =
    mutable.Map[String, (ToolSchema, ToolExecutor[? <: CallableTool[?]])]()

  def registerTool(
      schema: ToolSchema,
      executor: ToolExecutor[? <: CallableTool[?]]
  ): Unit = {
    tools(schema.name) = (schema, executor)
  }

  def getSchemas: List[ToolSchema] = tools.values.map(_._1).toList

  def getFunctionDefinitions: List[FunctionDefinition] = {
    getSchemas.map(schemaToFunctionDefinition)
  }

  def getTools: List[Tool] = {
    getFunctionDefinitions.map(funcDef => Tool("function", funcDef))
  }

  def getSchemasJson: String = {
    val schemas = getSchemas.map(ToolSchema.toJson)
    s"[${schemas.mkString(", ")}]"
  }

  private def schemaToFunctionDefinition(
      schema: ToolSchema
  ): FunctionDefinition = {
    val parametersJson = JsonSchema.toJson(schema.parameters)
    FunctionDefinition(
      name = schema.name,
      description = Some(schema.description),
      parameters = Some(parametersJson),
      strict = None
    )
  }

  def execute(toolName: String, args: Value): Option[Value] = {
    tools.get(toolName).map { case (_, executor) =>
      executor.execute(args)
    }
  }

  def clear(): Unit = tools.clear()

  def getRegisteredToolNames: List[String] = tools.keys.toList
}
