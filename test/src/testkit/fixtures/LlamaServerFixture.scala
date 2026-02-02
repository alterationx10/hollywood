package testkit.fixtures

import munit.FunSuite
import veil.Veil

/** Base fixture for tests that require a running llama-server instance.
  *
  * By default, tests are ignored. Set `munitIgnore = false` to run tests.
  */
trait LlamaServerFixture extends FunSuite {

  /** The LLM model to use for completions
    */
  val completionModel: String

  /** The model to use for embeddings. Defaults to [[completionModel]]
    */
  lazy val embeddingModel: String = completionModel

  /** Whether tests should be ignored (default: true, requires manual enable) */
  override def munitIgnore: Boolean =
    Veil.get("HOLLYWOOD_IGNORE_LLAMA_FIXTURE").forall(_.toBoolean)

}
