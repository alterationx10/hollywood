# Hollywood

A library for building LLM agents in Scala 3.

Built for local LLMs, tested primarily with llama-server and gpt-oss. Works with OpenAI-compatible endpoints.

## Setup

Add to your `build.sbt` or Mill configuration:

```scala
"dev.alteration" %% "hollywood" % "0.0.15"
```

### Configuration

Set environment variables for your LLM server:

- `LLAMA_SERVER_URL` - Base URL for the LLM server (default: `http://localhost:8080`)
- `LLAMA_SERVER_COMPLETION_URL` - Chat completions endpoint (falls back to `LLAMA_SERVER_URL`)
- `LLAMA_SERVER_EMBEDDING_URL` - Embeddings endpoint (falls back to `LLAMA_SERVER_URL`)
- `SEARXNG_URL` - SearXNG search instance (default: `http://localhost:8888`)

Start llama-server with embeddings support (an example):

```bash
llama-server -hf ggml-org/gpt-oss-20b-GGUF --ctx-size 8192 --jinja -ub 2048 -b 2048 --embeddings --pooling mean
```

## Quick Start

### Basic Agent

```scala
import hollywood.*

val agent = OneShotAgent(
  systemPrompt = "You are a helpful assistant. Respond concisely."
)

val response = agent.chat("What is 2+2?")
println(response)
```

### Agent with Tools

```scala
import hollywood.tools.*
import hollywood.tools.provided.http.HttpClientTool

val toolRegistry = ToolRegistry()
  .register[HttpClientTool]

val agent = OneShotAgent(
  systemPrompt = "You are a helpful assistant with web access.",
  toolRegistry = Some(toolRegistry)
)

val response = agent.chat("Make a GET request to https://api.github.com/users/octocat")
```

## Agents

All agents implement a single interface:

```scala
trait Agent {
  def chat(message: String): String
}
```

### OneShotAgent

Stateless agent for single request-response cycles:

```scala
val agent = OneShotAgent(
  systemPrompt = "You are a helpful assistant."
)

val response = agent.chat("What is 2+2?")
```

Create task-specific agents:

```scala
val summarizer = OneShotAgent.forTask(
  taskName = "Text Summarization",
  taskDescription = "Summarize the given text in one sentence.",
  inputFormat = Some("Raw text"),
  outputFormat = Some("One sentence summary")
)

val summary = summarizer.chat("Long text goes here...")
```

### ConversationalAgent

Maintains conversation history:

```scala
val agent = ConversationalAgent()

val response1 = agent.chat("I have an orange cat named Whiskers.")
val response2 = agent.chat("What color was it?")
// Response will remember the cat is orange
```

Control history size:

```scala
val conversationState = new InMemoryState(maxMessages = 50)
val agent = ConversationalAgent(conversationState = conversationState)
```

### RagAgent

Retrieval-Augmented Generation with vector search:

```scala
val vectorStore = new InMemoryVectorStore()
val embeddingClient = new EmbeddingClient()
val documentIndexer = new DocumentIndexer(embeddingClient, vectorStore)

// Index documents
val documents = List(
  ("doc1", "Scala is a strong statically typed programming language..."),
  ("doc2", "The JVM enables running Java programs...")
)
documentIndexer.indexDocuments(documents)

// Create RAG agent
val ragAgent = new RagAgent(
  completionClient = ChatCompletionClient(),
  embeddingClient = embeddingClient,
  vectorStore = vectorStore,
  topK = 3
)

val answer = ragAgent.chat("What is Scala?")
```

### Agents as Tools

Use agents as tools within other agents:

```scala
val calculatorAgent = OneShotAgent(
  systemPrompt = "You are a calculator. Perform arithmetic accurately."
)

val calculatorTool = Agent.deriveAgentTool(
  calculatorAgent,
  agentName = Some("calculator"),
  description = "Use this tool to perform arithmetic calculations"
)

val toolRegistry = ToolRegistry()
  .register(calculatorTool)

val mainAgent = OneShotAgent(
  systemPrompt = "You are an assistant. When asked to do math, use the calculator tool.",
  toolRegistry = Some(toolRegistry)
)
```

## Tools

Define tools as case classes:

```scala
import hollywood.tools.*
import hollywood.tools.schema.*
import scala.util.{Try, Success, Failure}

@Tool("Calculate the area of a rectangle")
case class RectangleArea(
                          @Param("width of the rectangle") width: Double,
                          @Param("height of the rectangle") height: Double
                        ) extends CallableTool[Double] derives upickle.ReadWriter {
  def execute(): Try[Double] = {
    if (width < 0 || height < 0) {
      Failure(new IllegalArgumentException("Width and height must be positive"))
    } else {
      Success(width * height)
    }
  }
}

// Register and use
val toolRegistry = ToolRegistry()
  .register[RectangleArea]

val agent = OneShotAgent(
  systemPrompt = "You are a geometry assistant.",
  toolRegistry = Some(toolRegistry)
)
```

Tools automatically support any return type with a JSON encoder. The compiler validates this at compile time.

### Provided Tools

The library includes these tools:

- **HttpClientTool** - Make HTTP requests (GET, POST, PUT, DELETE, PATCH, HEAD)
- **FileSystemTool** - Read, write, list files (use with FileSystemPolicy)
- **RegexTool** - Pattern matching, extraction, replacement
- **JsonQueryTool** - Query and transform JSON data
- **WebFetch** - Fetch web pages
- **SearXNGTool** - Web search via SearXNG

Register them like any other tool:

```scala
import hollywood.tools.provided.http.HttpClientTool
import hollywood.tools.provided.json.JsonQueryTool

val toolRegistry = ToolRegistry()
  .register[HttpClientTool]
  .register[JsonQueryTool]
```

## Security

Use `ToolPolicy` to validate and restrict tool execution:

```scala
val policy = ToolPolicy.fromValidator[Calculator] { calc =>
  if (calc.a < 0 || calc.b < 0) {
    Failure(new SecurityException("Negative numbers not allowed"))
  } else {
    Success(())
  }
}

val executor = ToolExecutor.derived[Calculator]
val restricted = RestrictedExecutor(executor, policy)

val toolRegistry = ToolRegistry()
  .register(ToolSchema.derive[Calculator], restricted)
```

### FileSystem Security

Always use `FileSystemPolicy` with `FileSystemTool`:

```scala
import hollywood.tools.provided.fs.{FileSystemTool, FileSystemPolicy}
import java.nio.file.Paths

// Restrict to /tmp, read-only
val policy = FileSystemPolicy.strict(Paths.get("/tmp"))

val executor = ToolExecutor.derived[FileSystemTool]
val restricted = RestrictedExecutor(executor, policy)

val toolRegistry = ToolRegistry()
  .register(ToolSchema.derive[FileSystemTool], restricted)
```

Policy options:

- `strict(path)` - Read-only, sandboxed
- `default(path)` - Sandboxed with write access, 10MB file limit
- `permissive(path)` - 100MB file limit, minimal restrictions

Built-in protections block access to `.env`, `.key`, `.pem`, `.ssh`, and credential files.

## Testing

Tests require a running llama-server instance and are ignored by default.

To run tests:

1. Start llama-server (see Configuration above)

2. Enable tests by setting `munitIgnore = false` in test files:

```scala
class OneShotAgentSpec extends LlamaServerFixture {
  override def munitIgnore: Boolean = false
  // ... tests ...
}
```

Note: If you use a .env to load test env variables, note that mill runs tests sandboxed (not in project root).

You can do something like:

```shell
VEIL_ENV_DIR=`pwd` ./mill --jobs 1 test
```

Extra Note: Running all the jobs will likely overwhelm a local llm, and you can control the parallelism with `--jobs 1`
Test suites demonstrate:

- Basic agent usage (`OneShotAgentSpec`)
- RAG with document indexing (`RagAgentSpec`)
- Conversation history (`ConversationalAgentSpec`)
- Tool composition (`CallableToolSpec`)

## License

Apache 2.0
