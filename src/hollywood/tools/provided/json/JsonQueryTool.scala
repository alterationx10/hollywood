package hollywood.tools.provided.json

import ujson.*
import hollywood.tools.schema.Param
import hollywood.tools.{schema, CallableTool}
import upickle.default.ReadWriter

import scala.util.Try

@schema.Tool("Query, filter, and transform JSON data with path expressions")
case class JsonQueryTool(
    @Param("JSON string to query") json: String,
    @Param(
      "Operation to perform: 'get' (extract value at path), 'filter' (filter array by condition), 'map' (extract fields from array), 'keys' (list object keys), 'values' (list object values), 'exists' (check if path exists), 'validate' (check structure)"
    )
    operation: String,
    @Param(
      "JSONPath query (e.g., 'users.0.name', 'items.*.id', 'data.results'). Use dot notation for objects, numeric index for arrays, '*' for all array elements"
    )
    path: Option[String] = None,
    @Param(
      "Field name to extract for 'map' operation (e.g., 'id' to extract all IDs from array)"
    )
    field: Option[String] = None,
    @Param(
      "Expected type for 'validate' operation: 'object', 'array', 'string', 'number', 'boolean', 'null'"
    )
    expectedType: Option[String] = None
) extends CallableTool[String]
    derives ReadWriter {

  override def execute(): Try[String] = Try {
    val parsed = Try(ujson.read(json)).getOrElse(
      throw new IllegalArgumentException(s"Invalid JSON")
    )

    operation.toLowerCase match {
      case "get"      => getValue(parsed)
      case "filter"   => filterArray(parsed)
      case "map"      => mapArray(parsed)
      case "keys"     => getKeys(parsed)
      case "values"   => getValues(parsed)
      case "exists"   => checkExists(parsed)
      case "validate" => validateType(parsed)
      case _          =>
        throw new IllegalArgumentException(
          s"Unknown operation: $operation. Valid operations are: get, filter, map, keys, values, exists, validate"
        )
    }
  }

  // Navigate to a JSON value using a path
  private def navigatePath(json: Value, pathStr: String): Option[Value] = {
    val parts = pathStr.split("\\.").filter(_.nonEmpty)

    parts.foldLeft(Option(json)) { (current, part) =>
      current.flatMap { json =>
        if (part == "*") {
          // Wildcard - return the array itself, will be handled by caller
          Some(json)
        } else if (part.matches("\\d+")) {
          // Numeric index for arrays
          val index = part.toInt
          Try(json.arr).toOption.flatMap(arr =>
            if (index >= 0 && index < arr.size) Some(arr(index)) else None
          )
        } else {
          // Object field access
          Try(json.obj.get(part)).toOption.flatten
        }
      }
    }
  }

  // Extract value at specified path
  private def getValue(json: Value): String = {
    path match {
      case Some(p) =>
        if (p.contains("*")) {
          // Handle wildcard paths
          val beforeWildcard = p.substring(0, p.indexOf(".*"))
          val afterWildcard  =
            if (p.indexOf(".*") + 2 < p.length)
              Some(p.substring(p.indexOf(".*") + 2))
            else None

          val baseJson =
            if (beforeWildcard.isEmpty) json
            else
              navigatePath(json, beforeWildcard)
                .getOrElse(
                  throw new IllegalArgumentException(
                    s"Path not found: $beforeWildcard"
                  )
                )

          Try(baseJson.arr).toOption match {
            case Some(arr) =>
              val results = afterWildcard match {
                case Some(after) =>
                  arr.flatMap(item => navigatePath(item, after))
                case None        => arr
              }
              ujson.write(Arr(results), indent = 0)
            case None      =>
              throw new IllegalArgumentException(
                "Wildcard path requires an array"
              )
          }
        } else {
          navigatePath(json, p) match {
            case Some(value) => ujson.write(value, indent = 0)
            case None        =>
              throw new IllegalArgumentException(s"Path not found: $p")
          }
        }
      case None    => ujson.write(json, indent = 0)
    }
  }

  // Filter array elements based on field existence or value
  private def filterArray(json: Value): String = {
    val target = path match {
      case Some(p) =>
        navigatePath(json, p)
          .getOrElse(throw new IllegalArgumentException(s"Path not found: $p"))
      case None    => json
    }

    Try(target.arr).toOption match {
      case Some(arr) =>
        field match {
          case Some(f) =>
            // Filter by field existence
            val filtered = arr.filter { item =>
              Try(item.obj.contains(f)).getOrElse(false)
            }
            s"Filtered ${arr.size} items to ${filtered.size} items with field '$f':\n${ujson
                .write(Arr(filtered))}"
          case None    =>
            throw new IllegalArgumentException(
              "Field parameter required for filter operation"
            )
        }
      case None      =>
        throw new IllegalArgumentException("Filter operation requires an array")
    }
  }

  // Map array to extract specific field from each element
  private def mapArray(json: Value): String = {
    val target = path match {
      case Some(p) =>
        navigatePath(json, p)
          .getOrElse(throw new IllegalArgumentException(s"Path not found: $p"))
      case None    => json
    }

    Try(target.arr).toOption match {
      case Some(arr) =>
        field match {
          case Some(f) =>
            val mapped =
              arr.flatMap(item => Try(item.obj.get(f)).toOption.flatten)
            s"Extracted ${mapped.size} values for field '$f':\n${ujson.write(Arr(mapped))}"
          case None    =>
            throw new IllegalArgumentException(
              "Field parameter required for map operation"
            )
        }
      case None      =>
        throw new IllegalArgumentException("Map operation requires an array")
    }
  }

  // Get all keys from a JSON object
  private def getKeys(json: Value): String = {
    val target = path match {
      case Some(p) =>
        navigatePath(json, p)
          .getOrElse(throw new IllegalArgumentException(s"Path not found: $p"))
      case None    => json
    }

    Try(target.obj).toOption match {
      case Some(obj) =>
        val keys = obj.keys.toList.sorted
        s"Object has ${keys.size} key(s):\n${ujson.write(Arr(keys.map(Str(_))))}"
      case None      =>
        throw new IllegalArgumentException("Keys operation requires an object")
    }
  }

  // Get all values from a JSON object
  private def getValues(json: Value): String = {
    val target = path match {
      case Some(p) =>
        navigatePath(json, p)
          .getOrElse(throw new IllegalArgumentException(s"Path not found: $p"))
      case None    => json
    }

    Try(target.obj).toOption match {
      case Some(obj) =>
        val values = obj.values.toSeq
        s"Object has ${values.size} value(s):\n${ujson.write(Arr(values))}"
      case None      =>
        throw new IllegalArgumentException(
          "Values operation requires an object"
        )
    }
  }

  // Check if a path exists
  private def checkExists(json: Value): String = {
    path match {
      case Some(p) =>
        val exists = navigatePath(json, p).isDefined
        if (exists) s"true - Path '$p' exists"
        else s"false - Path '$p' does not exist"
      case None    =>
        throw new IllegalArgumentException(
          "Path parameter required for exists operation"
        )
    }
  }

  // Validate JSON type
  private def validateType(json: Value): String = {
    val target = path match {
      case Some(p) =>
        navigatePath(json, p)
          .getOrElse(throw new IllegalArgumentException(s"Path not found: $p"))
      case None    => json
    }

    expectedType match {
      case Some(t) =>
        val actualType   = getJsonType(target)
        val expectedNorm = t.toLowerCase
        val isValid      = actualType == expectedNorm

        if (isValid) s"true - Value is of type '$expectedNorm'"
        else s"false - Expected '$expectedNorm' but got '$actualType'"
      case None    =>
        val actualType = getJsonType(target)
        s"Value is of type '$actualType'"
    }
  }

  private def getJsonType(json: Value): String = json match {
    case Null    => "null"
    case Bool(_) => "boolean"
    case Num(_)  => "number"
    case Str(_)  => "string"
    case Arr(_)  => "array"
    case Obj(_)  => "object"
  }
}
