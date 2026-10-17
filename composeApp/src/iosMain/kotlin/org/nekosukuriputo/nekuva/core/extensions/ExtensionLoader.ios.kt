package org.nekosukuriputo.nekuva.core.extensions

/**
 * iOS enforces AOT code signing and does not permit dynamic loading of external JAR/DEX bytecode.
 * All 1,300+ parsers from `nekuva-exts` are statically compiled into the iOS binary.
 */
actual fun loadExtension(path: String): LoadedExtension? = null
