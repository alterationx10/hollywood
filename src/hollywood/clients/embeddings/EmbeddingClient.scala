package hollywood.clients.embeddings

import veil.Veil
import upickle.default.{write, read}

class EmbeddingClient(
    embeddingHandler: EmbeddingsRequest => EmbeddingsResponse =
      EmbeddingClient.defaultEmbeddingHandler,
    embeddingModel: String = "gpt-oss"
) {

  /** Get embedding for a text */
  def getEmbedding(text: String): List[Double] = {
    val request  = EmbeddingsRequest(
      input = ujson.Str(text),
      model = embeddingModel
    )
    val response = embeddingHandler(request)
    response.data.headOption
      .map(_.embedding)
      .getOrElse(throw new RuntimeException("Failed to get embedding"))
  }
}

object EmbeddingClient {

  val baseUrl: String =
    Veil
      .getFirst("LLAMA_SERVER_EMBEDDING_URL", "LLAMA_SERVER_URL")
      .getOrElse("http://localhost:8080")

  val defaultEmbeddingHandler: EmbeddingsRequest => EmbeddingsResponse = {
    req =>
      {
        println(s"baseurl $baseUrl")
        val jsonData = write(req)
        val response = requests.post(
          s"$baseUrl/v1/embeddings",
          data = jsonData,
          headers = Map("Content-Type" -> "application/json")
        )
        read[EmbeddingsResponse](response.text())
      }
  }
}
