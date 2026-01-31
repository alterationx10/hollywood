package hollywood.tools

import scala.util.Try

trait CallableTool[A] extends Product {
  def execute(): Try[A]
}
