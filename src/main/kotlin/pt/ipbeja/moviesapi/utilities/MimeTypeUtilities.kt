package pt.ipbeja.moviesapi.utilities

import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory


val mimeTypeMappings = mapOf(
    "jpg" to "image/jpg",
    "jpeg" to "image/jpg",
    "png" to "image/png",
    "gif" to "image/gif",
)


fun Path.getMimeType(): String {
    assert(!this.isDirectory()) { "Not a file" }
    return mimeTypeMappings[this.extension] ?: "application/octet"
}

fun String.getMimeType()= Path.of(this).getMimeType()