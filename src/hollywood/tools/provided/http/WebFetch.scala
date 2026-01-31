package hollywood.tools.provided.http

import hollywood.tools.{schema, CallableTool}
import hollywood.tools.schema.Param
import upickle.default.ReadWriter

import scala.util.Try

@schema.Tool("Fetch a webpage by url")
case class WebFetch(
    @Param("A valid url") url: String
) extends CallableTool[String]
    derives ReadWriter {

  override def execute(): Try[String] = Try {
    requests.get(url).text()
  }

}
