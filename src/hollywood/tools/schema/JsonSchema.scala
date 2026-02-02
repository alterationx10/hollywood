package hollywood.tools.schema

import scala.deriving.Mirror
import scala.compiletime.{erasedValue, summonInline}
import upickle.default.{Reader, Writer}
import ujson.Value

object JsonSchema {

  /** Derive a JSON Schema for a type T */
  inline def derived[T](using m: Mirror.Of[T]): Schema = {
    deriveSchema[T](using m)
  }

  /** Public API to get schema for a type */
  inline def of[T](using schema: Schema): Schema = schema

  /** Convert a Schema to JSON (ujson.Value) */
  def toJson(schema: Schema): Value = {
    schema match {
      case Schema.ObjectSchema(properties, required, description) =>
        val propsJson = ujson.Obj.from(
          properties.map { case (name, propSchema) =>
            name -> toJson(propSchema)
          }
        )
        val obj       = ujson.Obj(
          "type"       -> ujson.Str("object"),
          "properties" -> propsJson
        )
        if (required.nonEmpty) {
          obj("required") = ujson.Arr.from(required.map(ujson.Str(_)))
        }
        description.foreach(d => obj("description") = ujson.Str(d))
        obj

      case Schema.StringSchema(description) =>
        val obj = ujson.Obj("type" -> ujson.Str("string"))
        description.foreach(d => obj("description") = ujson.Str(d))
        obj

      case Schema.NumberSchema(description) =>
        val obj = ujson.Obj("type" -> ujson.Str("number"))
        description.foreach(d => obj("description") = ujson.Str(d))
        obj

      case Schema.IntegerSchema(description) =>
        val obj = ujson.Obj("type" -> ujson.Str("integer"))
        description.foreach(d => obj("description") = ujson.Str(d))
        obj

      case Schema.BooleanSchema(description) =>
        val obj = ujson.Obj("type" -> ujson.Str("boolean"))
        description.foreach(d => obj("description") = ujson.Str(d))
        obj

      case Schema.ArraySchema(items, description) =>
        val obj = ujson.Obj(
          "type"  -> ujson.Str("array"),
          "items" -> toJson(items)
        )
        description.foreach(d => obj("description") = ujson.Str(d))
        obj

      case Schema.EnumSchema(values, description) =>
        val obj = ujson.Obj(
          "type" -> ujson.Str("string"),
          "enum" -> ujson.Arr.from(values.map(ujson.Str(_)))
        )
        description.foreach(d => obj("description") = ujson.Str(d))
        obj
    }
  }

  // Internal derivation logic
  private inline def deriveSchema[T](using m: Mirror.Of[T]): Schema = {
    inline m match {
      case p: Mirror.ProductOf[T] =>
        deriveProductSchema[T](using p)
      case s: Mirror.SumOf[T]     =>
        // For sum types, we could derive enum schemas
        // For now, just use a string schema
        Schema.StringSchema(None)
    }
  }

  private inline def deriveProductSchema[T](using
      p: Mirror.ProductOf[T]
  ): Schema = {
    val labels     = getLabels[p.MirroredElemLabels]
    val types      = getSchemas[p.MirroredElemTypes]
    val properties = labels.zip(types).toMap
    val required   = labels.toList // All fields required by default
    Schema.ObjectSchema(properties, required, None)
  }

  private inline def getLabels[T <: Tuple]: List[String] =
    inline erasedValue[T] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts)  =>
        constValue[t].asInstanceOf[String] :: getLabels[ts]
    }

  private inline def getSchemas[T <: Tuple]: List[Schema] =
    inline erasedValue[T] match {
      case _: EmptyTuple => Nil
      case _: (t *: ts)  =>
        getSchemaFor[t] :: getSchemas[ts]
    }

  private inline def getSchemaFor[T]: Schema = {
    inline erasedValue[T] match {
      case _: String      => Schema.StringSchema(None)
      case _: Int         => Schema.IntegerSchema(None)
      case _: Long        => Schema.IntegerSchema(None)
      case _: Double      => Schema.NumberSchema(None)
      case _: Float       => Schema.NumberSchema(None)
      case _: Boolean     => Schema.BooleanSchema(None)
      case _: Option[t]   => getSchemaFor[t] // Unwrap Option
      case _: List[t]     => Schema.ArraySchema(getSchemaFor[t], None)
      case _: Seq[t]      => Schema.ArraySchema(getSchemaFor[t], None)
      case _: Vector[t]   => Schema.ArraySchema(getSchemaFor[t], None)
      case _: ujson.Value =>
        Schema.ObjectSchema(Map.empty, List.empty, None) // JSON value
      case _              =>
        // For other types, try to derive recursively
        summonInline[Mirror.Of[T]] match {
          case m: Mirror.Of[T] => deriveSchema[T](using m)
        }
    }
  }

  private inline def constValue[T]: Any =
    inline erasedValue[T] match {
      case _: EmptyTuple => compiletime.error("EmptyTuple has no value")
      case _             => compiletime.constValue[T]
    }

}
