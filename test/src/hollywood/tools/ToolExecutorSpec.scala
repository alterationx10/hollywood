package hollywood.tools

import hollywood.tools.schema.Param
import munit.FunSuite
import upickle.default.ReadWriter

class ToolExecutorSpec extends FunSuite {

  test("ToolExecutor should decode Json arguments correctly") {
    // Define a test tool
    @schema.Tool("Test tool with multiple parameter types")
    case class TestTool(
        @Param("a string parameter") stringParam: String,
        @Param("an integer parameter") intParam: Int,
        @Param("a boolean parameter") boolParam: Boolean
    ) extends CallableTool[String]
        derives ReadWriter {
      def execute(): scala.util.Try[String] =
        scala.util.Success(s"$stringParam-$intParam-$boolParam")
    }

    // Create the executor
    val executor = ToolExecutor.derived[TestTool]

    // Create Json arguments
    val jsonArgs = ujson.Obj(
      "stringParam" -> ujson.Str("hello"),
      "intParam"    -> ujson.Num(42),
      "boolParam"   -> ujson.Bool(true)
    )

    // Execute
    val result = executor.execute(jsonArgs)

    // Verify - result should be Json
    assertEquals(result, ujson.Str("hello-42-true"))
  }

  test("ToolExecutor should handle URL strings without quotes") {
    @schema.Tool("URL test tool")
    case class UrlTestTool(
        @Param("A URL") url: String
    ) extends CallableTool[String]
        derives ReadWriter {
      def execute(): scala.util.Try[String] = scala.util.Success(url)
    }

    val executor = ToolExecutor.derived[UrlTestTool]

    val jsonArgs = ujson.Obj(
      "url" -> ujson.Str("https://example.com")
    )

    val result = executor.execute(jsonArgs)
    assertEquals(result, ujson.Str("https://example.com"))
  }

  test("ToolExecutor should return Json for numeric results") {
    @schema.Tool("Math tool")
    case class MathTool(
        @Param("a number") x: Int,
        @Param("a number") y: Int
    ) extends CallableTool[Int]
        derives ReadWriter {
      def execute(): scala.util.Try[Int] = scala.util.Success(x + y)
    }

    val executor = ToolExecutor.derived[MathTool]
    val jsonArgs = ujson.Obj(
      "x" -> ujson.Num(10),
      "y" -> ujson.Num(32)
    )

    val result = executor.execute(jsonArgs)
    assertEquals(result, ujson.Num(42))
  }
}
