package com.axel_stein.document_crop

import android.annotation.SuppressLint
import android.graphics.Rect
import android.graphics.RectF
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

class DObjectDetector(
    private val previewWidth: Int,
    private val previewHeight: Int
) : ImageAnalysis.Analyzer {
    private val objectDetector: ObjectDetector
    var listener: Listener? = null

    init {
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .build()
            .also {
                objectDetector = ObjectDetection.getClient(it)
            }
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val image = imageProxy.image
        if (image == null) {
            imageProxy.close()
            return
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(image, rotation)

        objectDetector.process(inputImage)
            .addOnSuccessListener { objects ->
                val listener = listener ?: return@addOnSuccessListener
                // In order to correctly display the bounds, the orientation of the analyzed
                // image and that of the viewfinder have to match. Which is why the dimensions of
                // the analyzed image are reversed if its rotation information is 90 or 270.
                val reverseDimens = rotation == 90 || rotation == 270
                val width = if (reverseDimens) imageProxy.height else imageProxy.width
                val height = if (reverseDimens) imageProxy.width else imageProxy.height

                val bounds = objects.map { it.boundingBox.transform(width, height) }
                listener.onDocumentsDetected(bounds)
                imageProxy.close()
            }
            .addOnFailureListener {
                listener?.onError(it)
                imageProxy.close()
            }
    }

    private fun Rect.transform(width: Int, height: Int): RectF {
        val scaleX = previewWidth / width.toFloat()
        val scaleY = previewHeight / height.toFloat()

        // Scale all coordinates to match preview
        val scaledLeft = scaleX * left
        val scaledTop = scaleY * top
        val scaledRight = scaleX * right
        val scaledBottom = scaleY * bottom
        return RectF(scaledLeft, scaledTop, scaledRight, scaledBottom)
    }

    interface Listener {
        fun onDocumentsDetected(bounds: List<RectF>)
        fun onError(exception: Exception)
    }
}