
package com.mediatek.encapsulation.com.mediatek.banyan.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.mediatek.banyan.widget.MTKImageView;

public class EncapsulatedMTKImageView extends MTKImageView {

    /**
     * @param context The Context to attach
     */
    public EncapsulatedMTKImageView(Context context) {
        super(context);
    }

    /**
     * @param context The Context to attach
     * @param attrs The attribute set
     */
    public EncapsulatedMTKImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * @param context The Context to attach
     * @param attrs The attribute set
     * @param defStyle The used style
     */
    public EncapsulatedMTKImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}
