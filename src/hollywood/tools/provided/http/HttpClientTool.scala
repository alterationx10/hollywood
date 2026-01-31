package hollywood.tools.provided.http

import ujson.Value
import hollywood.tools.{schema, CallableTool}
import hollywood.tools.schema.Param
import upickle.default.ReadWriter

import scala.util.Try

@schema.Tool("Make HTTP requests to any API endpoint")
case class HttpClientTool(
    @Param("The URL to request") url: String,
    @Param(
      "HTTP method (GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS)"
    ) method: String = "GET",
    @Param("Optional request headers as JSON object") headers: Option[String] =
      None,
    @Param("Optional request body as string") body: Option[String] = None
) extends CallableTool[String]
    derives ReadWriter {

  override def execute(): Try[String] = Try {
    // Parse headers if provided
    val headerMap = headers.flatMap { headerJson =>
      scala.util.Try(ujson.read(headerJson)).toOption match {
        case Some(obj: ujson.Obj) =>
          Some(obj.obj.collect {
            case (key, ujson.Str(value)) => key -> value
          }.toMap)
        case _                    => None
      }
    }.getOrElse(Map.empty[String, String])

    // Make HTTP request based on method
    val response = method.toUpperCase match {
      case "GET"     => requests.get(url, headers = headerMap)
      case "POST"    =>
        requests.post(url, data = body.getOrElse(""), headers = headerMap)
      case "PUT"     =>
        requests.put(url, data = body.getOrElse(""), headers = headerMap)
      case "DELETE"  => requests.delete(url, headers = headerMap)
      case "PATCH"   =>
        requests.patch(url, data = body.getOrElse(""), headers = headerMap)
      case "HEAD"    => requests.head(url, headers = headerMap)
      case "OPTIONS" =>
        // requests library doesn't support OPTIONS - fallback to GET
        requests.get(url, headers = headerMap)
      case _         => requests.get(url, headers = headerMap) // Default to GET
    }

    response.text()
  }

}
