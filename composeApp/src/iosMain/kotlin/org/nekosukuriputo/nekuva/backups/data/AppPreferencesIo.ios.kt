package org.nekosukuriputo.nekuva.backups.data

import platform.Foundation.NSNumber
import platform.Foundation.NSUserDefaults

actual fun dumpAppPreferences(): Map<String, Any?> = runCatching {
    val dict = NSUserDefaults.standardUserDefaults.dictionaryRepresentation()
    val out = LinkedHashMap<String, Any?>()
    for ((k, v) in dict) {
        val key = k as? String ?: continue
        if (key.startsWith("NS") || key.startsWith("Apple")) continue
        when (v) {
            is String -> out[key] = v
            is Boolean -> out[key] = v
            is Int -> out[key] = v
            is Long -> out[key] = v
            is Float -> out[key] = v
            is Double -> out[key] = v
            is NSNumber -> out[key] = v.stringValue
            is List<*> -> {
                val strings = v.mapNotNull { it as? String }.toSet()
                if (strings.isNotEmpty()) out[key] = strings
            }
        }
    }
    out
}.getOrDefault(emptyMap())
