package testkit.fixtures

import munit.FunSuite

/** Base fixture for tests that require a running llama-server instance.
  *
  * By default, tests are ignored. Set `munitIgnore = false` to run tests.
  * Set `shouldStartLlamaServer = true` to have the fixture manage the server.
  */
trait LlamaServerFixture extends FunSuite {

  /** Whether tests should be ignored (default: true, requires manual enable) */
  override def munitIgnore: Boolean = true

  /** Whether the fixture should start/stop llama-server (default: false, expects external server) */
  val shouldStartLlamaServer: Boolean = false

  // Note: Server management not implemented - tests expect external llama-server
  // Start with: llama-server -hf ggml-org/gpt-oss-20b-GGUF --ctx-size 8192 --jinja -ub 2048 -b 2048 --embeddings --pooling mean
}
