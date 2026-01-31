package hollywood.tools.provided.http

import hollywood.tools.ToolRegistry
import testkit.fixtures.HttpBinContainerSuite
import munit.FunSuite

class HttpClientToolSpec extends HttpBinContainerSuite {

  test("HttpClientTool should execute a GET request") {
    val tool   = HttpClientTool(url = s"${getContainerUrl}/get", method = "GET")
    val result = tool.execute()
    println(result)
    assert(result.isSuccess, "Request should succeed")
    result.foreach { body =>
      assert(
        body.contains(s"$getContainerUrl"),
        s"Response should contain $getContainerUrl"
      )
    }
  }

  test("HttpClientTool should execute a POST request with body") {
    val tool   = HttpClientTool(
      url = s"${getContainerUrl}/post",
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
      url = s"${getContainerUrl}/headers",
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
      url = s"${getContainerUrl}/put",
      method = "PUT",
      body = Some("""{"key": "value"}""")
    )
    val result = tool.execute()
    assert(result.isSuccess, "Request should succeed")
  }

  test("HttpClientTool should execute a DELETE request") {
    val tool   = HttpClientTool(
      url = s"${getContainerUrl}/delete",
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
        "dev.alteration.branch.hollywood.tools.provided.http.HttpClientTool"
      ),
      s"Registry should contain HttpClientTool. Got: $tools"
    )
  }

  test("HttpClientTool should execute via ToolRegistry") {

    val registry = ToolRegistry()
    registry.register[HttpClientTool]

    val args = ujson.Obj(
      "url"    -> ujson.Str(s"${getContainerUrl}/get"),
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
        value.contains(s"$getContainerUrl"),
        s"Result should contain $getContainerUrl"
      )
    }
  }

  test("HttpClientTool should default to GET when method is unknown") {
    val tool   = HttpClientTool(
      url = s"${getContainerUrl}/get",
      method = "INVALID"
    )
    val result = tool.execute()
    assert(result.isSuccess, "Request should succeed with default GET method")
  }
}
