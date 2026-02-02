package hollywood

import testkit.fixtures.LlamaServerFixture
import veil.Veil

class OneShotAgentSpec extends LlamaServerFixture {

  override val completionModel: String =
    Veil.get("HOLLYWOOD_COMPLETION_MODEL").getOrElse("gpt-oss-20b")

  test("OneShotAgent should respond to basic chat messages") {
    val agent = OneShotAgent(
      systemPrompt = "You are a helpful assistant. Respond concisely.",
      model = completionModel
    )

    val response = agent.chat("What is 2+2?")
    assert(response.nonEmpty)
    assert(response.contains("4"))
  }

  test(
    "OneShotAgent.forTask should create agent with structured task definition"
  ) {
    val summarizer = OneShotAgent.forTask(
      taskName = "Text Summarization",
      taskDescription = "Summarize the given text in one sentence.",
      inputFormat = Some("Raw text"),
      outputFormat = Some("One sentence summary"),
      model = completionModel
    )

    val text = "Artificial intelligence has made tremendous progress. " +
      "AI systems are becoming increasingly capable in many domains. " +
      "Code generation and documentation is a great example."

    val summary = summarizer.chat(text)

    assert(summary.nonEmpty)
    assert(summary.length < text.length)
  }

  test("OneShotAgent.execute should support variable substitution") {
    val agent = OneShotAgent(
      systemPrompt = "You are a greeting generator.",
      model = completionModel
    )

    val template = "Generate a {style} greeting for {name}."
    val result   = agent.execute(
      template,
      Map("style" -> "formal", "name" -> "Dr. Smith")
    )

    assert(result.nonEmpty)
    assert(!result.contains("{style}"))
    assert(!result.contains("{name}"))
  }
}
