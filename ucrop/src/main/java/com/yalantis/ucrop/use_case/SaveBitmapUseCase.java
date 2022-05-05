package com.yalantis.ucrop.use_case;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;

import com.yalantis.ucrop.util.BitmapLoadUtils;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * Crops part of image that fills the crop bounds.
 * <p/>
 * First image is downscaled if max size was set and if resulting image is larger that max size.
 * Then image is rotated accordingly.
 * Finally new Bitmap object is created and saved to file.
 */
public class SaveBitmapUseCase {
    private Context mContext;
    private Bitmap mViewBitmap;
    private CompressFormat mCompressFormat;
    private int mCompressQuality;
    private Uri mImageOutputUri;
    private RectF mCropRect;
    private RectF mCurrentImageRect;
    private float mCurrentScale;
    private float mCurrentAngle;
    private int mMaxResultImageSizeX;
    private int mMaxResultImageSizeY;

    private SaveBitmapUseCase() {}

    public static class Builder {
        private final Context mContext;
        private Bitmap mViewBitmap;
        private CompressFormat mCompressFormat = CompressFormat.JPEG;
        private int mCompressQuality = 100;
        private Uri mImageOutputUri;
        private RectF mCropRect;
        private RectF mCurrentImageRect;
        private float mCurrentScale;
        private float mCurrentAngle;
        private int mMaxResultImageSizeX;
        private int mMaxResultImageSizeY;

        public Builder(Context context) {
            mContext = context.getApplicationContext();
        }

        public Builder setBitmap(Bitmap bitmap) {
            mViewBitmap = bitmap;
            return this;
        }

        public Builder setCompressFormat(CompressFormat format) {
            mCompressFormat = format;
            return this;
        }

        public Builder setCompressQuality(int quality) {
            mCompressQuality = quality;
            return this;
        }

        public Builder setOutputUri(Uri uri) {
            mImageOutputUri = uri;
            return this;
        }

        public Builder setCropRect(RectF rect) {
            mCropRect = rect;
            return this;
        }

        public Builder setImageRect(RectF rect) {
            mCurrentImageRect = rect;
            return this;
        }

        public Builder setScale(float scale) {
            mCurrentScale = scale;
            return this;
        }

        public Builder setAngle(float angle) {
            mCurrentAngle = angle;
            return this;
        }

        public Builder setMaxResultImageSizeX(int sizeX) {
            mMaxResultImageSizeX = sizeX;
            return this;
        }

        public Builder setMaxResultImageSizeY(int sizeY) {
            mMaxResultImageSizeY = sizeY;
            return this;
        }

        public SaveBitmapUseCase build() {
            SaveBitmapUseCase useCase = new SaveBitmapUseCase();
            useCase.mContext = mContext;
            useCase.mViewBitmap = mViewBitmap;
            useCase.mCompressFormat = mCompressFormat;
            useCase.mCompressQuality = mCompressQuality;
            useCase.mImageOutputUri = mImageOutputUri;
            useCase.mCropRect = mCropRect;
            useCase.mCurrentImageRect = mCurrentImageRect;
            useCase.mCurrentScale = mCurrentScale;
            useCase.mCurrentAngle = mCurrentAngle;
            useCase.mMaxResultImageSizeX = mMaxResultImageSizeX;
            useCase.mMaxResultImageSizeY = mMaxResultImageSizeY;
            return useCase;
        }
    }

    public void save() throws Exception {
        OutputStream outputStream = null;
        ByteArrayOutputStream outStream = null;
        try {
            outputStream = mContext.getContentResolver().openOutputStream(mImageOutputUri);
            outStream = new ByteArrayOutputStream();
            Bitmap croppedBitmap = crop();
            croppedBitmap.compress(mCompressFormat, mCompressQuality, outStream);
            outputStream.write(outStream.toByteArray());
            croppedBitmap.recycle();
        } catch (Exception e) {
            throw new Exception(e);
        } finally {
            BitmapLoadUtils.close(outputStream);
            BitmapLoadUtils.close(outStream);
        }
    }

    private Bitmap crop() {
        if (mViewBitmap == null) {
            throw new IllegalArgumentException("ViewBitmap is null");
        } else if (mViewBitmap.isRecycled()) {
            throw new IllegalArgumentException("ViewBitmap is recycled");
        } else if (mCurrentImageRect.isEmpty()) {
            throw new IllegalArgumentException("CurrentImageRect is empty");
        }

        // Downsize if needed
        if (mMaxResultImageSizeX > 0 && mMaxResultImageSizeY > 0) {
            float cropWidth = mCropRect.width() / mCurrentScale;
            float cropHeight = mCropRect.height() / mCurrentScale;

            if (cropWidth > mMaxResultImageSizeX || cropHeight > mMaxResultImageSizeY) {

                float scaleX = mMaxResultImageSizeX / cropWidth;
                float scaleY = mMaxResultImageSizeY / cropHeight;
                float resizeScale = Math.min(scaleX, scaleY);

                Bitmap resizedBitmap = Bitmap.createScaledBitmap(mViewBitmap,
                        Math.round(mViewBitmap.getWidth() * resizeScale),
                        Math.round(mViewBitmap.getHeight() * resizeScale), false);
                if (mViewBitmap != resizedBitmap) {
                    mViewBitmap.recycle();
                }
                mViewBitmap = resizedBitmap;

                mCurrentScale /= resizeScale;
            }
        }

        // Rotate if needed
        if (mCurrentAngle != 0) {
            Matrix tempMatrix = new Matrix();
            tempMatrix.setRotate(mCurrentAngle, (float) mViewBitmap.getWidth() / 2, (float) mViewBitmap.getHeight() / 2);

            Bitmap rotatedBitmap = Bitmap.createBitmap(mViewBitmap, 0, 0, mViewBitmap.getWidth(), mViewBitmap.getHeight(),
                    tempMatrix, true);
            if (mViewBitmap != rotatedBitmap) {
                mViewBitmap.recycle();
            }
            mViewBitmap = rotatedBitmap;
        }

        int cropOffsetX = Math.round((mCropRect.left - mCurrentImageRect.left) / mCurrentScale);
        int cropOffsetY = Math.round((mCropRect.top - mCurrentImageRect.top) / mCurrentScale);
        int mCroppedImageWidth = Math.round(mCropRect.width() / mCurrentScale);
        int mCroppedImageHeight = Math.round(mCropRect.height() / mCurrentScale);

        boolean shouldCrop = shouldCrop(mCroppedImageWidth, mCroppedImageHeight);

        if (shouldCrop) {
            return Bitmap.createBitmap(mViewBitmap, cropOffsetX, cropOffsetY, mCroppedImageWidth, mCroppedImageHeight);
        } else {
            return mViewBitmap;
        }
    }

    /**
     * Check whether an image should be cropped at all or just file can be copied to the destination path.
     * For each 1000 pixels there is one pixel of error due to matrix calculations etc.
     *
     * @param width  - crop area width
     * @param height - crop area height
     * @return - true if image must be cropped, false - if original image fits requirements
     */
    private boolean shouldCrop(int width, int height) {
        int pixelError = 1;
        pixelError += Math.round(Math.max(width, height) / 1000f);
        return (mMaxResultImageSizeX > 0 && mMaxResultImageSizeY > 0)
                || Math.abs(mCropRect.left - mCurrentImageRect.left) > pixelError
                || Math.abs(mCropRect.top - mCurrentImageRect.top) > pixelError
                || Math.abs(mCropRect.bottom - mCurrentImageRect.bottom) > pixelError
                || Math.abs(mCropRect.right - mCurrentImageRect.right) > pixelError
                || mCurrentAngle != 0;
    }

}
