package org.nekosukuriputo.nekuva.reader.domain

import org.jetbrains.skia.Image

actual fun decodeImageBounds(bytes: ByteArray): Pair<Int, Int>? = runCatching {
    val img = Image.makeFromEncoded(bytes)
    img.width to img.height
}.getOrNull()
