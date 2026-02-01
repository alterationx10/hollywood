package testkit.fixtures

import munit.*
import veil.Veil

trait HttpBinSuite extends FunSuite {

  val httpBinUrl: String =
    Veil.get("HTTPBIN_URL").getOrElse("https://httpbin.org")

}
