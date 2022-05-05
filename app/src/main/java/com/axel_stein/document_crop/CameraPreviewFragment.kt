package com.axel_stein.document_crop

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRectF
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.axel_stein.document_crop.databinding.FragmentCameraPreviewBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraPreviewFragment : Fragment() {
    private lateinit var viewBinding: FragmentCameraPreviewBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private lateinit var objectDetector: ObjectDetector
    private lateinit var imageAnalysis: ImageAnalysis
    private val viewModel: CroppedPreviewViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        objectDetector = createObjectDetector()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentCameraPreviewBinding.inflate(inflater)
        viewBinding.capture.setOnClickListener {
            capturePhoto()
        }
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestCameraPermission()
    }

    private fun requestCameraPermission() {
        val permissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { permitted ->
            if (permitted) {
                startCamera()
            } else {
                showToast("Camera is not permitted")
            }
        }
        permissionRequest.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        imageCapture = ImageCapture.Builder().build()
        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        setDocumentDetector(imageAnalysis)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(viewBinding.cameraPreview.surfaceProvider)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            val cameraProvider = cameraProviderFuture.get()
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis, imageCapture)
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun createObjectDetector(): ObjectDetector {
        return ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .build()
            .run {
                ObjectDetection.getClient(this)
            }
    }

    private fun setDocumentDetector(imageAnalysis: ImageAnalysis) {
        viewBinding.cameraPreview.previewStreamState.observe(viewLifecycleOwner) { streamState ->
            if (streamState != PreviewView.StreamState.STREAMING) {
                return@observe
            }

            val preview = viewBinding.cameraPreview
            val width = preview.width
            val height = preview.height

            imageAnalysis.setAnalyzer(cameraExecutor, createDocumentDetector(width, height))
        }
    }

    private fun createDocumentDetector(previewWidth: Int, previewHeight: Int): ImageAnalysis.Analyzer {
        val detector = DObjectDetector(previewWidth, previewHeight)
        detector.listener = object : DObjectDetector.Listener {
            override fun onDocumentsDetected(bounds: List<RectF>) {
                viewBinding.boundsOverlay.post {
                    viewBinding.boundsOverlay.drawBounds(bounds)
                }
            }

            override fun onError(exception: Exception) {
                throw Exception(exception)
            }
        }
        return detector
    }

    private fun capturePhoto() {
        val imageCapture = imageCapture ?: return
        showLoader(true)
        imageCapture.takePicture(ContextCompat.getMainExecutor(requireContext()), object : ImageCapture.OnImageCapturedCallback() {
            @SuppressLint("UnsafeOptInUsageError")
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                val image = imageProxy.image
                if (image == null) {
                    showLoader(false)
                } else {
                    lifecycleScope.launch {
                        detectDocumentOpenPreview(
                            image.toBitmap(),
                            imageProxy.imageInfo.rotationDegrees
                        )
                        imageProxy.close()
                    }
                }
            }
        })
    }

    private fun detectDocumentOpenPreview(bitmap: Bitmap, rotation: Int) {
        val image = InputImage.fromBitmap(bitmap, rotation)
        objectDetector.process(image)
            .addOnSuccessListener { objects ->
                showLoader(false)
                objects.firstOrNull()?.run {
                    openCroppedPreview(bitmap, boundingBox, rotation)
                }
            }
            .addOnFailureListener {
                it.printStackTrace()
                showLoader(false)
            }
    }

    private fun openCroppedPreview(bitmap: Bitmap, box: Rect, rotation: Int) {
        lifecycleScope.launch {
            viewModel.setCropData(bitmap.rotate(rotation), box.toRectF())
            findNavController().navigate(R.id.action_open_cropped_preview)
        }
    }

    private fun showLoader(show: Boolean) {
        viewBinding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}