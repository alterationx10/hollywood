package hollywood.tools

import ujson.Value
import upickle.default.{Reader, Writer, read, writeJs}

import scala.deriving.Mirror
import scala.language.implicitConversions
import scala.util.Try

trait ToolExecutor[T <: CallableTool[?]] {
  def execute(args: Value): Value
}

object ToolExecutor {

  // Match type to extract the result type A from CallableTool[A]
  type ResultType[T <: CallableTool[?]] <: Any = T match {
    case CallableTool[a] => a
  }

  // Private implementation to avoid anonymous class duplication at inline sites
  private[tools] class DerivedExecutor[T <: CallableTool[?]](
      reader: Reader[T],
      writer: Writer[ResultType[T]]
  ) extends ToolExecutor[T] {
    override def execute(args: Value): Value = {
      Try(read[T](args)(using reader)) match {
        case scala.util.Success(tool) =>
          tool.execute() match {
            case scala.util.Success(result) =>
              result match {
                case s: String => ujson.Str(s) // Handle String separately - seems to double quote otherwise
                case _         => writeJs(result.asInstanceOf[ResultType[T]])(using writer)
              }
            case scala.util.Failure(e)      =>
              ujson.Str(s"Error executing tool: ${e.getMessage}")
          }
        case scala.util.Failure(e)    =>
          ujson.Str(s"Error decoding tool arguments: ${e.getMessage}")
      }
    }
  }

  inline def derived[T <: CallableTool[?]](using
      m: Mirror.ProductOf[T],
      reader: Reader[T],
      writer: Writer[ResultType[T]]
  ): ToolExecutor[T] = {
    new DerivedExecutor[T](reader, writer)
  }

}
