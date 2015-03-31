package com.mediatek.encapsulation.android.drm;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.mediatek.drm.OmaDrmUiUtils;
import com.mediatek.encapsulation.EncapsulationConstant;
import com.mediatek.encapsulation.MmsLog;

public class EncapsulatedDrmUiUtils {

    private static final String TAG = "OmaDrmUiUtils";

    /**
     * Overlay back ground bitmap with front picture
     * note that the OmaDrmClient instance shall be created in an Acitivty context
     *
     * @param client OmaDrmClient instance
     * @param bgdBmp Background bitmap to draw on
     * @param front Foreground drawable to draw
     * @return Bitmap New bitmap with overlayed drawable
     */
    public static Bitmap overlayBitmap(
            EncapsulatedDrmManagerClient client, Bitmap bgdBmp, Drawable front) {
        if (EncapsulationConstant.USE_MTK_PLATFORM) {
            return OmaDrmUiUtils.overlayBitmap(client, bgdBmp, front);
        } else {
            if (null == bgdBmp || null == front || null == client) {
                MmsLog.e(TAG, "overlayBitmap : invalid parameters");
                return null;
            }

            Bitmap bMutable = Bitmap.createBitmap(bgdBmp.getWidth(),
                                                  bgdBmp.getHeight(),
                                                  bgdBmp.getConfig());
            Canvas overlayCanvas = new Canvas(bMutable);
             // make sure the bitmap is valid otherwise we use an empty one
            if (!bgdBmp.isRecycled()) {
                overlayCanvas.drawBitmap(bgdBmp, 0, 0, null);
            }
            int overlayWidth = front.getIntrinsicWidth();
            int overlayHeight = front.getIntrinsicHeight();
            int left = bgdBmp.getWidth() - overlayWidth;
            int top = bgdBmp.getHeight() - overlayHeight;
            Rect newBounds = new Rect(left, top, left + overlayWidth, top + overlayHeight);
            front.setBounds(newBounds);
            front.draw(overlayCanvas);
            return bMutable;
        }
    }

}
