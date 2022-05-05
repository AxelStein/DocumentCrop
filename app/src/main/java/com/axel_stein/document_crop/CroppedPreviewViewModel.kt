package com.axel_stein.document_crop

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yalantis.ucrop.use_case.SaveBitmapUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CroppedPreviewViewModel : ViewModel() {
    private val data = MutableLiveData<CropData?>()
    val cropLiveData = data as LiveData<CropData?>

    private val showMessage = MutableLiveData<Event<String>>()
    val showMessageLiveData = showMessage as LiveData<Event<String>>

    private val closePreview = MutableLiveData<Event<Boolean>>()
    val closePreviewLiveData = closePreview as LiveData<Event<Boolean>>

    private val loading = MutableLiveData<Boolean>()
    val loadingLiveData = loading as LiveData<Boolean>

    fun setCropData(bitmap: Bitmap, cropRect: RectF) {
        data.value = CropData(bitmap, cropRect)
    }

    fun clearCropData() {
        data.value = null
    }

    fun save(useCase: SaveBitmapUseCase) {
        loading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                useCase.save()
                showMessage.postValue(Event("Saved"))
                closePreview.postValue(Event(true))
            } catch (e: Exception) {
                e.printStackTrace()
                showMessage.postValue(Event("Error saving"))
            } finally {
                loading.postValue(false)
            }
        }
    }
}

data class CropData(
    val bitmap: Bitmap,
    val cropRect: RectF
)

class Event<T>(private var data: T?) {
    private var consumed = false

    fun consume(): T? {
        if (consumed) {
            this.data = null
            return null
        }
        consumed = true
        return data
    }
}