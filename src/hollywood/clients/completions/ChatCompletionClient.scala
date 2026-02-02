package hollywood.clients.completions

import veil.Veil
import ChatCompletionsRequest.given
import upickle.default.{read, write}
import ujson.Value

class ChatCompletionClient(
    completionHandler: ChatCompletionsRequest => ChatCompletionsResponse =
      ChatCompletionClient.defaultCompletionHandler
) {

  def getCompletion(
      request: ChatCompletionsRequest
  ): ChatCompletionsResponse = {
    completionHandler(request)
  }
}

object ChatCompletionClient {

  val baseUrl: String = Veil
    .getFirst("LLAMA_SERVER_COMPLETION_URL", "LLAMA_SERVER_URL")
    .getOrElse("http://localhost:8080")

  val defaultCompletionHandler
      : ChatCompletionsRequest => ChatCompletionsResponse = { req =>
    {
      val jsonData = write(req)
      val response = requests.post(
        s"$baseUrl/v1/chat/completions",
        data = jsonData,
        headers = Map("Content-Type" -> "application/json")
      )
      read[ChatCompletionsResponse](response.text())
    }
  }

}
