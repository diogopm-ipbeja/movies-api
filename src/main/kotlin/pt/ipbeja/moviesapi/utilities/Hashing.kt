package pt.ipbeja.moviesapi.utilities

import at.favre.lib.crypto.bcrypt.BCrypt
import io.ktor.util.decodeBase64String
import io.ktor.util.encodeBase64
import kotlin.text.toCharArray

private val hasher = BCrypt.withDefaults()
private val verifier = BCrypt.verifyer()


fun String.hash() = hasher.hash(12, this.toByteArray()).encodeBase64()

fun String.verify(hash: String): BCrypt.Result = verifier.verify(this.toCharArray(), hash.decodeBase64String())

