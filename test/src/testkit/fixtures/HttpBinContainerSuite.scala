package testkit.fixtures

import munit.*

trait HttpBinContainerSuite extends FunSuite {

  // For now, these tests are disabled as they require a running httpbin container
  // In a real setup, this would use testcontainers to spin up httpbin
  override def munitIgnore: Boolean = true

  // Base URL for httpbin service (when enabled)
  val httpBinUrl: String = sys.env.getOrElse("HTTPBIN_URL", "http://localhost:8080")

  // Method for compatibility with test code
  def getContainerUrl: String = httpBinUrl
}
