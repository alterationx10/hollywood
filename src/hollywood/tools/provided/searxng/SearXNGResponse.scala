package hollywood.tools.provided.searxng

import upickle.default.{ReadWriter, macroRW}

case class SearchResult(
    title: String,
    url: Option[String] = None,
    content: String,
    engine: String,
    score: Option[Double] = None,
    publishedDate: Option[String] = None
) derives ReadWriter

case class SearXNGResponse(
    query: String,
    results: List[SearchResult]
) derives ReadWriter
