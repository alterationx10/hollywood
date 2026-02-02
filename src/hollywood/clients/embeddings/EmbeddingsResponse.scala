package hollywood.clients.embeddings

import upickle.default.{macroRW, ReadWriter}

case class EmbeddingsResponse(
    `object`: String, // "list"
    data: List[Embedding],
    model: String,
    usage: EmbeddingUsage
) derives ReadWriter

case class Embedding(
    `object`: String,        // "embedding"
    embedding: List[Double], // The embedding vector
    index: Int
) derives ReadWriter

case class EmbeddingUsage(
    prompt_tokens: Int,
    total_tokens: Int
) derives ReadWriter
