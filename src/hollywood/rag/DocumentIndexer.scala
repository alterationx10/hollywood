package hollywood.rag

import hollywood.clients.embeddings.EmbeddingClient
import hollywood.rag.VectorStore

class DocumentIndexer(
    embeddingClient: EmbeddingClient,
    vectorStore: VectorStore
) {

  /** Index documents into the vector store */
  def indexDocuments(documents: List[(String, String)]): Unit = {
    documents.foreach { case (id, content) =>
      val embedding = embeddingClient.getEmbedding(content)
      vectorStore.add(id, content, embedding)
    }
  }
}
