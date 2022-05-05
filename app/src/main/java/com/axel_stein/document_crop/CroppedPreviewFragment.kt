package com.axel_stein.document_crop

import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.axel_stein.document_crop.databinding.FragmentCroppedPreviewBinding
import com.yalantis.ucrop.view.OverlayView.FREESTYLE_CROP_MODE_ENABLE
import java.text.SimpleDateFormat
import java.util.*

class CroppedPreviewFragment : Fragment() {
    private lateinit var viewBinding: FragmentCroppedPreviewBinding
    private val viewModel: CroppedPreviewViewModel by activityViewModels()
    private val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
        if (destination.id != R.id.cropped_preview_fragment) {
            viewModel.clearCropData()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)

        viewBinding = FragmentCroppedPreviewBinding.inflate(inflater)
        viewBinding.closePreview.setOnClickListener {
            closeCroppedPreview()
        }
        viewBinding.ucrop.overlayView.freestyleCropMode = FREESTYLE_CROP_MODE_ENABLE
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.cropLiveData.observe(viewLifecycleOwner) { data ->
            if (data != null) {
                viewBinding.ucrop.setImageBitmap(data.bitmap, data.cropRect)
            }
        }
        viewModel.showMessageLiveData.observe(viewLifecycleOwner) { event ->
            event.consume()?.run {
                showToast(this)
            }
        }
        viewModel.closePreviewLiveData.observe(viewLifecycleOwner) { event ->
            event.consume()?.run {
                closeCroppedPreview()
            }
        }
        viewModel.loadingLiveData.observe(viewLifecycleOwner) { loading ->
            showLoader(loading)
        }
    }

    override fun onResume() {
        super.onResume()
        findNavController().addOnDestinationChangedListener(listener)
    }

    override fun onPause() {
        super.onPause()
        findNavController().removeOnDestinationChangedListener(listener)
    }

    private fun closeCroppedPreview() {
        findNavController().navigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save -> {
                saveCroppedPhoto()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun saveCroppedPhoto() {
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        val uri = requireContext().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        viewModel.save(viewBinding.ucrop.createSaveBitmapUseCase(uri))
    }

    private fun showLoader(show: Boolean) {
        viewBinding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}