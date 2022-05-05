package com.axel_stein.document_crop

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun Bitmap.rotate(angle: Int): Bitmap {
    return withContext(Dispatchers.IO) {
        val matrix = Matrix().apply { postRotate(angle.toFloat()) }
        Bitmap.createBitmap(this@rotate, 0, 0, width, height, matrix, true)
    }
}

suspend fun Image.toBitmap(): Bitmap {
    return withContext(Dispatchers.IO) {
        val buffer = planes[0].buffer
        buffer.rewind()
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}