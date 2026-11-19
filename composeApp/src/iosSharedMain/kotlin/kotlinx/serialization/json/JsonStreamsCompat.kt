package kotlinx.serialization.json

import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy

@ExperimentalSerializationApi
fun <T> Json.encodeToStream(
    serializer: SerializationStrategy<T>,
    value: T,
    stream: OutputStream,
) {
    stream.write(encodeToString(serializer, value).encodeToByteArray())
}

@ExperimentalSerializationApi
fun <T> Json.decodeToSequence(
    stream: InputStream,
    deserializer: DeserializationStrategy<T>,
    format: DecodeSequenceMode = DecodeSequenceMode.AUTO_DETECT,
): Sequence<T> {
    val text = stream.readBytes().decodeToString().trim()
    if (text.isEmpty()) return emptySequence()
    val elem = parseToJsonElement(text)
    return when (elem) {
        is JsonArray -> elem.asSequence().map { decodeFromJsonElement(deserializer, it) }
        else -> sequenceOf(decodeFromJsonElement(deserializer, elem))
    }
}
