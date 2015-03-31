package com.mediatek.contacts.editor;

import android.content.Context;
import android.util.AttributeSet;

import com.android.contacts.R;

public class SimPhotoEditorViewPurple extends SimPhotoEditorView {

    public SimPhotoEditorViewPurple(Context context) {
        super(context);
    }

    public SimPhotoEditorViewPurple(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
    protected int getPhotoImageResource() {
        return R.drawable.mtk_ic_contact_picture_sim_contact_purple;
    }
}