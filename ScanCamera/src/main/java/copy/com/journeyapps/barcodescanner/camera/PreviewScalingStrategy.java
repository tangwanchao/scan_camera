package copy.com.journeyapps.barcodescanner.camera;

import android.graphics.Rect;
import android.util.Log;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import copy.com.journeyapps.barcodescanner.Size;

/**
 *
 */
public abstract class PreviewScalingStrategy {
    private static final String TAG = PreviewScalingStrategy.class.getSimpleName();

    /**
     * Choose the best preview size, based on our viewfinder size.
     * <p>
     * The default implementation lets subclasses calculate a score for each size, the picks the one
     * with the best score.
     * <p>
     * The sizes list may be reordered by this call.
     *
     * @param sizes   supported preview sizes, containing at least one size. Sizes are in natural camera orientation.
     * @param desired The desired viewfinder size, in the same orientation
     * @return the best preview size, never null
     */
    public Size getBestPreviewSize(List<Size> sizes, final Size desired) {
        // Sample of supported preview sizes:
        // http://www.kirill.org/ar/ar.php

        List<Size> ordered = getBestPreviewOrder(sizes, desired);

        Log.i(TAG, "Viewfinder size: " + desired);
        Log.i(TAG, "Preview in order of preference: " + ordered);
        for (Size size : ordered) {
            if (size.width / desired.width == size.height / desired.height) {
                Log.i(TAG, "查找到最佳分辨率: " + size);
                return size;
            }
        }
        for (Size size : ordered) {
            if (size.width >= desired.width && size.height >= desired.height) {
                Log.i(TAG, "查找到次要分辨率: " + size);
                return size;
            }
        }
        return ordered.get(0);
    }

    public Size getBestPhotoSize(List<Size> sizes, Size previewSize, final Size min) {
        List<Size> ordered = getBestPreviewOrder(sizes, previewSize);
        Log.i(TAG, "所有排序后照片分辨率: " + ordered);
        for (Size size : ordered) {
            if (size.width >= min.width && size.height >= min.height) {
                Log.i(TAG, "查找到最佳照片分辨率: " + size);
                return size;
            }
        }
        return ordered.get(0);
    }

    /**
     * Sort previews based on their suitability.
     * <p>
     * In most cases, {@link #getBestPreviewSize(List, Size)} should be used instead.
     * <p>
     * The sizes list may be reordered by this call.
     *
     * @param sizes   supported preview sizes, containing at least one size. Sizes are in natural camera orientation.
     * @param desired The desired viewfinder size, in the same orientation
     * @return an ordered list, best preview first
     */
    public List<Size> getBestPreviewOrder(List<Size> sizes, final Size desired) {
        if (desired == null) {
            return sizes;
        }

        Collections.sort(sizes, new Comparator<Size>() {
            @Override
            public int compare(Size a, Size b) {
                float aScore = getScore(a, desired);
                float bScore = getScore(b, desired);
                // Bigger score first
                return Float.compare(bScore, aScore);
            }
        });


        return sizes;
    }

    /**
     * Get a score for our size.
     * <p>
     * 1.0 is perfect (exact match).
     * 0.0 means we can't use it at all.
     * <p>
     * Subclasses should override this.
     *
     * @param size    the camera preview size (that can be scaled)
     * @param desired the viewfinder size
     * @return the score
     */
    protected float getScore(Size size, Size desired) {
        return 0.5f;
    }

    /**
     * Scale and position the preview relative to the viewfinder.
     *
     * @param previewSize    the size of the preview (camera), in current display orientation
     * @param viewfinderSize the size of the viewfinder (display), in current display orientation
     * @return a rect placing the preview, relative to the viewfinder
     */
    public abstract Rect scalePreview(Size previewSize, Size viewfinderSize);
}
