package hollywood.clients.completions

import upickle.default.{macroRW, Reader, ReadWriter, Writer}
import ujson.Value

import scala.util.Try

case class ChatCompletionsRequest(
    messages: List[ChatMessage],
    model: String,
    frequency_penalty: Option[Double] = None,
    logit_bias: Option[Map[String, Double]] = None,
    logprobs: Option[Boolean] = None,
    top_logprobs: Option[Int] = None,
    max_tokens: Option[Int] = None,
    n: Option[Int] = None,
    presence_penalty: Option[Double] = None,
    response_format: Option[ResponseFormat] = None,
    seed: Option[Int] = None,
    service_tier: Option[String] = None,
    stop: Option[Value] = None,        // Can be string or array of strings
    stream: Option[Boolean] = None,
    stream_options: Option[StreamOptions] = None,
    temperature: Option[Double] = None,
    tool_choice: Option[Value] = None, // Can be string or ToolChoice object
    tools: Option[List[Tool]] = None,
    top_p: Option[Double] = None,
    user: Option[String] = None,
    // llama.cpp specific parameters
    chat_template_kwargs: Option[Value] = None,
    reasoning_format: Option[String] = None,
    parse_tool_calls: Option[Boolean] = None
) derives ReadWriter

object ChatCompletionsRequest {
  // Map[String, Double] ReadWriter is automatically derived by uPickle
}

case class ChatMessage(
    role: String,                            // "system", "user", "assistant", "tool"
    content: Option[String] = None,          // Can be string or array of ContentPart
    name: Option[String] = None,
    tool_calls: Option[List[ToolCall]] = None,
    tool_call_id: Option[String] = None,
    reasoning_content: Option[String] = None // llama.cpp reasoning content
) derives ReadWriter

case class ContentPart(
    `type`: String, // "text" or "image_url"
    text: Option[String] = None,
    image_url: Option[ImageUrl] = None
) derives ReadWriter

case class ImageUrl(
    url: String,
    detail: Option[String] = None // "auto", "low", "high"
) derives ReadWriter

case class Tool(
    `type`: String, // "function"
    function: FunctionDefinition
) derives ReadWriter

case class FunctionDefinition(
    name: String,
    description: Option[String] = None,
    parameters: Option[Value] = None, // JSON Schema object
    strict: Option[Boolean] = None
) derives ReadWriter

case class ToolCall(
    id: String,
    `type`: String, // "function"
    function: FunctionCall
) derives ReadWriter

case class FunctionCall(
    name: String,
    arguments: String // JSON string
) derives ReadWriter {

  /** Parse arguments as Json */
  def argumentsJson: Value =
    Try(ujson.read(arguments.translateEscapes()))
      .getOrElse(ujson.Obj())

}

case class ToolChoice(
    `type`: String, // "function"
    function: ToolChoiceFunction
) derives ReadWriter

case class ToolChoiceFunction(
    name: String
) derives ReadWriter

case class ResponseFormat(
    `type`: String, // "text", "json_object", "json_schema"
    json_schema: Option[JsonSchema] = None
) derives ReadWriter

case class JsonSchema(
    name: String,
    description: Option[String] = None,
    schema: Value,
    strict: Option[Boolean] = None
) derives ReadWriter

case class StreamOptions(
    include_usage: Option[Boolean] = None
) derives ReadWriter
