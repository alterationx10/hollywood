package hollywood.clients.completions

import upickle.default.{macroRW, ReadWriter}

case class ChatCompletionsResponse(
    id: String,
    `object`: String, // "chat.completion" or "chat.completion.chunk"
    created: Long,
    model: String,
    system_fingerprint: Option[String] = None,
    choices: List[Choice],
    usage: Option[Usage] = None,
    // llama.cpp specific fields
    timings: Option[Timings] = None
) derives ReadWriter

case class Choice(
    index: Int,
    message: Option[ChatMessage] = None, // Present in non-streaming
    delta: Option[Delta] = None,         // Present in streaming
    logprobs: Option[ChoiceLogprobs] = None,
    finish_reason: Option[String] =
      None // "stop", "length", "tool_calls", "content_filter", "function_call"
) derives ReadWriter

case class Delta(
    role: Option[String] = None,
    content: Option[String] = None,
    tool_calls: Option[List[DeltaToolCall]] = None
) derives ReadWriter

case class DeltaToolCall(
    index: Int,
    id: Option[String] = None,
    `type`: Option[String] = None,
    function: Option[DeltaFunction] = None
) derives ReadWriter

case class DeltaFunction(
    name: Option[String] = None,
    arguments: Option[String] = None
) derives ReadWriter

case class ChoiceLogprobs(
    content: Option[List[ContentLogprob]] = None,
    refusal: Option[List[ContentLogprob]] = None
) derives ReadWriter

case class ContentLogprob(
    token: String,
    logprob: Double,
    bytes: Option[List[Int]] = None,
    top_logprobs: List[TopLogprob]
) derives ReadWriter

case class TopLogprob(
    token: String,
    logprob: Double,
    bytes: Option[List[Int]] = None
) derives ReadWriter

case class Usage(
    prompt_tokens: Int,
    completion_tokens: Int,
    total_tokens: Int,
    prompt_tokens_details: Option[PromptTokensDetails] = None,
    completion_tokens_details: Option[CompletionTokensDetails] = None
) derives ReadWriter

case class PromptTokensDetails(
    cached_tokens: Option[Int] = None,
    audio_tokens: Option[Int] = None
) derives ReadWriter

case class CompletionTokensDetails(
    reasoning_tokens: Option[Int] = None,
    audio_tokens: Option[Int] = None,
    accepted_prediction_tokens: Option[Int] = None,
    rejected_prediction_tokens: Option[Int] = None
) derives ReadWriter

case class Timings(
    prompt_n: Int,
    prompt_ms: Double,
    prompt_per_token_ms: Double,
    prompt_per_second: Double,
    predicted_n: Int,
    predicted_ms: Double,
    predicted_per_token_ms: Double,
    predicted_per_second: Double
) derives ReadWriter
