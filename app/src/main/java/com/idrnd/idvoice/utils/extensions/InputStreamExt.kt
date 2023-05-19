package com.idrnd.idvoice.utils.extensions

import java.io.InputStream

/**
 * Read in chunks until the condition is true.
 *
 * @param chunkSize size of output chunks.
 * @param whileCondition condition to continue reading. If returns false then reading will be stopped as soon as possible.
 */
fun InputStream.readInChunksWhile(chunkSize: Int, whileCondition: () -> Boolean): Sequence<ByteArray> {
    return sequence {
        val chunk = ByteArray(chunkSize)
        while (whileCondition.invoke()) {
            while (this@readInChunksWhile.available() < chunkSize) {
                if (!whileCondition.invoke()) return@sequence
            }
            this@readInChunksWhile.read(chunk)
            this@sequence.yield(chunk)
        }
    }
}
