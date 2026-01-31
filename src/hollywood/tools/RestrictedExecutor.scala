package hollywood.tools

import ujson.Value
import upickle.default.{Reader, read}

import scala.util.{Failure, Success, Try}

/** A generic ToolExecutor wrapper that enforces a ToolPolicy
  *
  * @param delegate
  *   The underlying tool executor
  * @param policy
  *   The security/validation policy to enforce
  * @param reader
  *   JSON reader for the tool type
  * @tparam T
  *   The CallableTool type
  */
class RestrictedExecutor[T <: CallableTool[?]](
    delegate: ToolExecutor[T],
    policy: ToolPolicy[T]
)(using reader: Reader[T])
    extends ToolExecutor[T] {

  override def execute(args: Value): Value = {
    // Allow policy to transform args first (e.g., sanitize, add defaults)
    val transformedArgs = policy.transformArgs(args)

    // Decode the tool
    Try(read[T](transformedArgs)(using reader)) match {
      case Success(tool) =>
        // Validate against policy before executing
        policy.validate(tool) match {
          case Success(_)     =>
            // Policy allows this operation - execute with transformed args
            delegate.execute(transformedArgs)
          case Failure(error) =>
            // Policy violation - return error
            ujson.Str(s"Policy violation: ${error.getMessage}")
        }

      case Failure(error) =>
        ujson.Str(s"Error decoding tool arguments: ${error.getMessage}")
    }
  }
}

object RestrictedExecutor {

  /** Create a RestrictedExecutor with a custom policy
    *
    * @param delegate
    *   The underlying tool executor
    * @param policy
    *   The policy to enforce
    * @param reader
    *   JSON reader for the tool type
    * @tparam T
    *   The CallableTool type
    * @return
    *   A new RestrictedExecutor instance
    */
  def apply[T <: CallableTool[?]](
      delegate: ToolExecutor[T],
      policy: ToolPolicy[T]
  )(using reader: Reader[T]): RestrictedExecutor[T] =
    new RestrictedExecutor[T](delegate, policy)
}
