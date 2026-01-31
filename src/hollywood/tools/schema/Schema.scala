package hollywood.tools.schema

import upickle.default.{ReadWriter, macroRW, Reader, Writer}
import ujson.Value

/** JSON Schema ADT for tool parameters */
enum Schema derives ReadWriter {
  case ObjectSchema(
      properties: Map[String, Schema],
      required: List[String],
      description: Option[String]
  )
  case StringSchema(description: Option[String])
  case NumberSchema(description: Option[String])
  case IntegerSchema(description: Option[String])
  case BooleanSchema(description: Option[String])
  case ArraySchema(items: Schema, description: Option[String])
  case EnumSchema(values: List[String], description: Option[String])
}

object Schema {
  // Export case classes for easy access
  export Schema.*
}
