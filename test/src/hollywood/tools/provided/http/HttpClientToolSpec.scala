package hollywood.tools.provided.http

import hollywood.tools.ToolRegistry
import testkit.fixtures.HttpBinSuite
import munit.FunSuite

class HttpClientToolSpec extends HttpBinSuite {

  test("HttpClientTool should execute a GET request") {
    val tool   = HttpClientTool(url = s"${httpBinUrl}/get", method = "GET")
    val result = tool.execute()
    println(result)
    assert(result.isSuccess, "Request should succeed")
    result.foreach { body =>
      assert(
        body.contains(s"$httpBinUrl"),
        s"Response should contain $httpBinUrl"
      )
    }
  }

  test("HttpClientTool should execute a POST request with body") {
    val tool   = HttpClientTool(
      url = s"${httpBinUrl}/post",
      method = "POST",
      body = Some("test data")
    )
    val result = tool.execute()
    assert(result.isSuccess, "Request should succeed")
    result.foreach { body =>
      assert(body.contains("test data"), "Response should contain posted data")
    }
  }

  test("HttpClientTool should include custom headers") {
    val headers = """{"X-Custom-Header": "test-value"}"""
    val tool    = HttpClientTool(
      url = s"${httpBinUrl}/headers",
      method = "GET",
      headers = Some(headers)
    )
    val result  = tool.execute()
    assert(result.isSuccess, "Request should succeed")
    result.foreach { body =>
      assert(
        body.contains("X-Custom-Header"),
        "Response should contain custom header"
      )
    }
  }

  test("HttpClientTool should execute a PUT request") {
    val tool   = HttpClientTool(
      url = s"${httpBinUrl}/put",
      method = "PUT",
      body = Some("""{"key": "value"}""")
    )
    val result = tool.execute()
    assert(result.isSuccess, "Request should succeed")
  }

  test("HttpClientTool should execute a DELETE request") {
    val tool   = HttpClientTool(
      url = s"${httpBinUrl}/delete",
      method = "DELETE"
    )
    val result = tool.execute()
    assert(result.isSuccess, "Request should succeed")
  }

  test("HttpClientTool should register with ToolRegistry") {
    val registry = ToolRegistry()
    registry.register[HttpClientTool]

    val tools = registry.getRegisteredToolNames
    assert(
      tools.contains(
        "hollywood.tools.provided.http.HttpClientTool"
      ),
      s"Registry should contain HttpClientTool. Got: $tools"
    )
  }

  test("HttpClientTool should execute via ToolRegistry") {

    val registry = ToolRegistry()
    registry.register[HttpClientTool]

    val args = ujson.Obj(
      "url"    -> ujson.Str(s"${httpBinUrl}/get"),
      "method" -> ujson.Str("GET")
    )

    val result = registry.execute(
      "hollywood.tools.provided.http.HttpClientTool",
      args
    )
    assert(result.isDefined, "Execution should return a result")
    result.foreach { v =>
      val value = ujson.write(v, indent = 0).stripPrefix("\"").stripSuffix("\"")
      assert(
        value.contains(s"$httpBinUrl"),
        s"Result should contain $httpBinUrl"
      )
    }
  }

  test("HttpClientTool should default to GET when method is unknown") {
    val tool   = HttpClientTool(
      url = s"${httpBinUrl}/get",
      method = "INVALID"
    )
    val result = tool.execute()
    assert(result.isSuccess, "Request should succeed with default GET method")
  }
}
