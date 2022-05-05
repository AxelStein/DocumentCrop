package com.axel_stein.document_crop

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

class DBoundsOverlay constructor(context: Context?, attributeSet: AttributeSet?) : View(context, attributeSet) {
    private val bounds: MutableList<RectF> = mutableListOf()
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context!!, android.R.color.holo_blue_light)
        strokeWidth = 4f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bounds.forEach { canvas.drawRect(it, paint) }
    }

    fun drawBounds(bounds: List<RectF>) {
        this.bounds.clear()
        this.bounds.addAll(bounds)
        invalidate()
    }
}