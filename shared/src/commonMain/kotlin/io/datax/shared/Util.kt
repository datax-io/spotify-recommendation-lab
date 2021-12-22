package io.datax.shared

import kotlinx.serialization.json.*

fun jsonObjectOf(vararg pairs: Pair<String, Any>): JsonObject = mapOf(*pairs).toJson().jsonObject

fun jsonArrayOf(vararg elements: Any): JsonArray = elements.map(Any::toJson).toJson().jsonArray

fun Any.toJson(): JsonElement = when (this) {
    is JsonElement -> this
    is String -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is Map<*, *> -> JsonObject(this.entries.associate { (key, value) -> (key as String) to (value as Any).toJson() })
    is List<*> -> JsonArray(this.map { (it as Any).toJson() })
    else -> throw Exception("Unsupported data type $this")
}
