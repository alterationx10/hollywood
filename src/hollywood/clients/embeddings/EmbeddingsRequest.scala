package hollywood.clients.embeddings

import upickle.default.{ReadWriter, macroRW}
import ujson.Value

case class EmbeddingsRequest(
    input: Value,                           // Can be string or array of strings
    model: String,
    encoding_format: Option[String] = None, // "float", "base64"
    dimensions: Option[Int] = None,
    user: Option[String] = None
) derives ReadWriter
