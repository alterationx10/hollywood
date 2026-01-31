package hollywood.tools

import hollywood.tools.provided.fs.{FileSystemTool, FileSystemPolicy}
import testkit.fixtures.FileFixtureSuite
import java.nio.file.Files

class FileSystemPolicySpec extends FileFixtureSuite {

  tmpDir.test("FileSystemPolicy should allow operations within sandbox") { dir =>
    val policy   = FileSystemPolicy(sandboxPath = Some(dir))
    val registry = ToolRegistry().registerWithPolicy[FileSystemTool](policy)

    val testFile = dir.resolve("test.txt")
    Files.writeString(testFile, "Hello")
    val args = ujson.Obj(
      "operation" -> ujson.Str("read"),
      "path"      -> ujson.Str(testFile.toString)
    )

    val result = registry.execute(
      "hollywood.tools.provided.fs.FileSystemTool",
      args
    )

    result match {
      case Some(v) => {
        val content = ujson.write(v, indent = 0).stripPrefix("\"").stripSuffix("\"")
        assertEquals(content, "Hello", "Should read file content")
      }
      case _ => fail("Expected successful read")
    }
  }

  tmpDir.test("FileSystemPolicy should block operations outside sandbox") { dir =>
    val policy   = FileSystemPolicy(sandboxPath = Some(dir))
    val registry = ToolRegistry().registerWithPolicy[FileSystemTool](policy)
    val args = ujson.Obj(
      "operation" -> ujson.Str("read"),
      "path"      -> ujson.Str("/etc/passwd")
    )

    val result = registry.execute(
      "hollywood.tools.provided.fs.FileSystemTool",
      args
    )

    result match {
      case Some(v) => {
        val error = ujson.write(v, indent = 0).stripPrefix("\"").stripSuffix("\"")
        assert(
          error.contains("outside sandbox"),
          s"Should block access outside sandbox. Got: $error"
        )
      }
      case _ => fail("Expected security error")
    }
  }

  tmpDir.test("FileSystemPolicy should block write operations when read-only") { dir =>
    val policy   = FileSystemPolicy(sandboxPath = Some(dir), readOnly = true)
    val registry = ToolRegistry().registerWithPolicy[FileSystemTool](policy)
    val args = ujson.Obj(
      "operation" -> ujson.Str("write"),
      "path"      -> ujson.Str(dir.resolve("test.txt").toString),
      "content"   -> ujson.Str("test")
    )

    val result = registry.execute(
      "hollywood.tools.provided.fs.FileSystemTool",
      args
    )

    result match {
      case Some(v) => {
        val error = ujson.write(v, indent = 0).stripPrefix("\"").stripSuffix("\"")
        assert(
          error.contains("read-only") || error.contains("disabled"),
          s"Should block write in read-only mode. Got: $error"
        )
      }
      case _ => fail("Expected security error")
    }
  }

  tmpDir.test("FileSystemPolicy should block paths with blocked patterns") { dir =>
    val policy   = FileSystemPolicy(
      sandboxPath = Some(dir),
      blockedPatterns = List(".env", ".key")
    )
    val registry = ToolRegistry().registerWithPolicy[FileSystemTool](policy)
    val args = ujson.Obj(
      "operation" -> ujson.Str("read"),
      "path"      -> ujson.Str(dir.resolve(".env").toString)
    )

    val result = registry.execute(
      "hollywood.tools.provided.fs.FileSystemTool",
      args
    )

    result match {
      case Some(v) => {
        val error = ujson.write(v, indent = 0).stripPrefix("\"").stripSuffix("\"")
        assert(
          error.contains("blocked pattern"),
          s"Should block .env file. Got: $error"
        )
      }
      case _ => fail("Expected security error")
    }
  }

  tmpDir.test("FileSystemPolicy should enforce max file size on writes") { dir =>
    val policy   = FileSystemPolicy(
      sandboxPath = Some(dir),
      maxFileSize = 100
    )
    val registry = ToolRegistry().registerWithPolicy[FileSystemTool](policy)
    val largeContent = "x" * 200
    val args         = ujson.Obj(
      "operation" -> ujson.Str("write"),
      "path"      -> ujson.Str(dir.resolve("large.txt").toString),
      "content"   -> ujson.Str(largeContent)
    )

    val result = registry.execute(
      "hollywood.tools.provided.fs.FileSystemTool",
      args
    )

    result match {
      case Some(v) => {
        val error = ujson.write(v, indent = 0).stripPrefix("\"").stripSuffix("\"")
        assert(
          error.contains("exceeds maximum"),
          s"Should block large file write. Got: $error"
        )
      }
      case _ => fail("Expected security error")
    }
  }

  tmpDir.test("FileSystemPolicy.strict should be read-only and sandboxed") { dir =>
    val policy = FileSystemPolicy.strict(dir)

    assertEquals(policy.readOnly, true, "Strict policy should be read-only")
    assertEquals(
      policy.sandboxPath,
      Some(dir),
      "Strict policy should have sandbox"
    )
  }

  tmpDir.test("FileSystemPolicy.permissive should allow writes") { dir =>
    val policy = FileSystemPolicy.permissive(Some(dir))

    assertEquals(
      policy.readOnly,
      false,
      "Permissive policy should allow writes"
    )
    assert(
      policy.maxFileSize > 10 * 1024 * 1024,
      "Permissive policy should have larger file size limit"
    )
  }

  tmpDir.test(
    "FileSystemPolicy should allow read operations in read-only mode"
  ) { dir =>
    val policy   = FileSystemPolicy(sandboxPath = Some(dir), readOnly = true)
    val registry = ToolRegistry().registerWithPolicy[FileSystemTool](policy)

    val testFile = dir.resolve("readonly-test.txt")
    Files.writeString(testFile, "Read-only test")
    val args = ujson.Obj(
      "operation" -> ujson.Str("read"),
      "path"      -> ujson.Str(testFile.toString)
    )

    val result = registry.execute(
      "hollywood.tools.provided.fs.FileSystemTool",
      args
    )

    result match {
      case Some(v) => {
        val content = ujson.write(v, indent = 0).stripPrefix("\"").stripSuffix("\"")
        assertEquals(
          content,
          "Read-only test",
          "Should allow read in read-only mode"
        )
      }
      case _ => fail("Expected successful read")
    }
  }

  tmpDir.test(
    "FileSystemPolicy should block path traversal attempts (../) in sandbox"
  ) { dir =>
    val policy   = FileSystemPolicy(sandboxPath = Some(dir))
    val registry = ToolRegistry().registerWithPolicy[FileSystemTool](policy)
    val args = ujson.Obj(
      "operation" -> ujson.Str("read"),
      "path"      -> ujson.Str(s"${dir.toString}/../../../etc/passwd")
    )

    val result = registry.execute(
      "hollywood.tools.provided.fs.FileSystemTool",
      args
    )

    result match {
      case Some(v) => {
        val error = ujson.write(v, indent = 0).stripPrefix("\"").stripSuffix("\"")
        assert(
          error.contains("outside sandbox"),
          s"Should block path traversal attempt. Got: $error"
        )
      }
      case _ => fail("Expected security error")
    }
  }

  tmpDir.test(
    "FileSystemPolicy should allow non-write operations with no sandbox"
  ) { dir =>
    val policy = FileSystemPolicy(sandboxPath = None)

    val testFile = dir.resolve("no-sandbox-test.txt")
    Files.writeString(testFile, "No sandbox")

    val result = policy.validatePath("read", testFile.toString, None)
    assert(result.isSuccess, "Should allow read when no sandbox is set")
  }

  tmpDir.test("FileSystemPolicy should block patterns case-insensitively") { dir =>
    val policy   = FileSystemPolicy(
      sandboxPath = Some(dir),
      blockedPatterns = List(".env")
    )
    val registry = ToolRegistry().registerWithPolicy[FileSystemTool](policy)
    val args = ujson.Obj(
      "operation" -> ujson.Str("read"),
      "path"      -> ujson.Str(dir.resolve(".ENV").toString)
    )

    val result = registry.execute(
      "hollywood.tools.provided.fs.FileSystemTool",
      args
    )

    result match {
      case Some(v) => {
        val error = ujson.write(v, indent = 0).stripPrefix("\"").stripSuffix("\"")
        assert(
          error.contains("blocked pattern"),
          s"Should block .ENV (uppercase) due to case-insensitive check. Got: $error"
        )
      }
      case _ => fail("Expected security error")
    }
  }

  tmpDir.test(
    "FileSystemPolicy should block patterns in subdirectories and filenames"
  ) { dir =>
    val policy   = FileSystemPolicy(
      sandboxPath = Some(dir),
      blockedPatterns = List("password")
    )
    val registry = ToolRegistry().registerWithPolicy[FileSystemTool](policy)
    val args = ujson.Obj(
      "operation" -> ujson.Str("read"),
      "path"      -> ujson.Str(
        dir.resolve("config/passwords.txt").toString
      )
    )

    val result = registry.execute(
      "hollywood.tools.provided.fs.FileSystemTool",
      args
    )

    result match {
      case Some(v) => {
        val error = ujson.write(v, indent = 0).stripPrefix("\"").stripSuffix("\"")
        assert(
          error.contains("blocked pattern"),
          s"Should block file with 'password' in path. Got: $error"
        )
      }
      case _ => fail("Expected security error")
    }
  }

  tmpDir.test("FileSystemPolicy.default should have reasonable restrictions") { dir =>
    val policy = FileSystemPolicy.default(dir)

    assertEquals(
      policy.sandboxPath,
      Some(dir),
      "Default policy should have sandbox"
    )
    assertEquals(
      policy.readOnly,
      false,
      "Default policy should allow writes"
    )
    assertEquals(
      policy.maxFileSize,
      10L * 1024L * 1024L,
      "Default policy should have 10MB file size limit"
    )
    assert(
      policy.blockedPatterns.contains(".env"),
      "Default policy should block .env files"
    )
  }
}
