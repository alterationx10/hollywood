package hollywood

import testkit.fixtures.LlamaServerFixture
import veil.Veil

class ConversationalAgentSpec extends LlamaServerFixture {

  override val completionModel: String =
    Veil.get("HOLLYWOOD_COMPLETION_MODEL").getOrElse("gpt-oss-20b")

  test("ConversationalAgent should maintain conversation history") {
    val agent     = ConversationalAgent(model = completionModel)
    val response1 = agent.chat("I have an orange cat named Whiskers.")
    assert(response1.nonEmpty)
    val response2 = agent.chat("What color was it?")
    assert(response2.nonEmpty)
    assert(
      response2.toLowerCase.contains("orange")
    )
  }

}
