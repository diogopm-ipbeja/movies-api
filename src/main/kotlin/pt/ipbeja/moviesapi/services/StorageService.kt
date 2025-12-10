@file:OptIn(ExperimentalUuidApi::class)

package pt.ipbeja.moviesapi.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.OpenOption
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


class StorageService(val baseDirectory: Path) {

    constructor(baseDirectory: String) : this(Path.of(baseDirectory))

    fun createTransaction(subDir: Path): StorageTransaction {
        val targetDirectory = (baseDirectory / subDir)
        return StorageTransaction(targetDirectory)
    }

    fun createTransaction(subDir: String): StorageTransaction {
        val targetDirectory = (baseDirectory / subDir)
        return StorageTransaction(targetDirectory)
    }

}

class StorageTransaction(private val targetPath: Path) {
    // private val files: MutableList<Pair<Path, ByteArray>> = mutableListOf()
    private val actions: MutableList<() -> Unit> = mutableListOf()
    private var isClosed = false
    private var isClosing = false

    fun addFile(filename: String, bytes: ByteArray): Path {
        if(isClosing || isClosed) throw IllegalStateException("Storage transaction is closing/closed")
        val extension = Path.of(filename).extension
        val filePath = targetPath / "${Uuid.random().toHexString()}.$extension"
        actions.add {
            if(!targetPath.exists()) targetPath.createDirectories()
            filePath.writeBytes(bytes)
        }
        return filePath
    }

    fun removeFile(path: String) {
        if(isClosing || isClosed) throw IllegalStateException("Storage transaction is closing/closed")
        val filePath = targetPath / path
        actions.add { filePath.deleteIfExists() }
    }

    fun commit() {
        if(isClosed) throw IllegalStateException("Storage transaction is closed")
        if(isClosing) return
        try {
            actions.forEach { it() }
        } catch (e: Exception) {
            rollback()
            throw e
        }
        isClosed = true
    }

    fun rollback() {
        if(isClosed) throw IllegalStateException("Storage transaction is closed")
        if(isClosing) return

        try {
            targetPath.deleteRecursively()
        } catch (_: Exception) {
            // ignored
        }
        actions.clear()
        isClosed = true
    }

}


suspend fun Path.writeAsync(bytes: ByteArray, vararg options: OpenOption) = withContext(Dispatchers.IO) {
    writeBytes(bytes, *options)
}

suspend fun Path.readAsync() = withContext(Dispatchers.IO) {
    readBytes()
}

suspend fun Path.deleteAsync() = withContext(Dispatchers.IO) {
    deleteIfExists()
}

