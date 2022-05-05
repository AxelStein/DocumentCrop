package com.yalantis.ucrop.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.yalantis.ucrop.R;
import com.yalantis.ucrop.callback.CropBoundsChangeListener;
import com.yalantis.ucrop.callback.InitImageSidesListener;
import com.yalantis.ucrop.callback.OverlayViewChangeListener;
import com.yalantis.ucrop.use_case.SaveBitmapUseCase;

public class UCropView extends FrameLayout {

    private GestureCropImageView mGestureCropImageView;
    private final OverlayView mViewOverlay;

    public UCropView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UCropView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(R.layout.ucrop_view, this, true);
        mGestureCropImageView = findViewById(R.id.image_view_crop);
        mViewOverlay = findViewById(R.id.view_overlay);

        mViewOverlay.processStyledAttributes();
        mGestureCropImageView.processStyledAttributes();

        setListenersToViews();
    }

    private void setListenersToViews() {
        mGestureCropImageView.setCropBoundsChangeListener(new CropBoundsChangeListener() {
            @Override
            public void onCropAspectRatioChanged(float cropRatio) {
                mViewOverlay.setTargetAspectRatio(cropRatio);
            }
        });
        mViewOverlay.setOverlayViewChangeListener(new OverlayViewChangeListener() {
            @Override
            public void onCropRectUpdated(RectF cropRect) {
                mGestureCropImageView.setCropRect(cropRect);
            }
        });
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @NonNull
    public GestureCropImageView getCropImageView() {
        return mGestureCropImageView;
    }

    @NonNull
    public OverlayView getOverlayView() {
        return mViewOverlay;
    }

    /**
     * Method for reset state for UCropImageView such as rotation, scale, translation.
     * Be careful: this method recreate UCropImageView instance and reattach it to layout.
     */
    public void resetCropImageView() {
        removeView(mGestureCropImageView);
        mGestureCropImageView = new GestureCropImageView(getContext());
        setListenersToViews();
        mGestureCropImageView.setCropRect(getOverlayView().getCropViewRect());
        addView(mGestureCropImageView, 0);
    }

    public void setImageBitmap(@NonNull final Bitmap bitmap, @NonNull final RectF initCropRect) {
        mGestureCropImageView.setInitImageSidesListener(new InitImageSidesListener() {
            @Override
            public void onInitImageSides(float width, float height) {
                int bitmapWidth = bitmap.getWidth();
                int bitmapHeight = bitmap.getHeight();

                float scaleX = bitmapWidth / width;
                float scaleY = bitmapHeight / height;

                RectF crop = new RectF(initCropRect);

                if (scaleX <= 1f && scaleY >= 1f) {
                    // image shrinked by width
                    crop.left *= scaleX;
                    crop.right *= scaleX;
                } else {
                    crop.left /= scaleX;
                    crop.right /= scaleX;
                }

                crop.top /= scaleY;
                crop.bottom /= scaleY;

                mViewOverlay.setCropViewRect(crop);
            }
        });
        mGestureCropImageView.setImageBitmap(bitmap);
    }

    public SaveBitmapUseCase createSaveBitmapUseCase(Uri outputUri) {
        return mGestureCropImageView.createSaveBitmapUseCase(outputUri);
    }
}