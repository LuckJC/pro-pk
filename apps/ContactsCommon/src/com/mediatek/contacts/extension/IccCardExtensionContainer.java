package com.mediatek.contacts.extension;

import java.util.LinkedList;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;

import com.mediatek.contacts.ext.IccCardExtension;
import com.mediatek.telephony.SimInfoManager.SimInfoRecord;

public class IccCardExtensionContainer extends IccCardExtension {

    private LinkedList<IccCardExtension> mSubExtensionList = new LinkedList<IccCardExtension>();
    
    public void add(IccCardExtension extension) {
        mSubExtensionList.add(extension);
    }
    
    public void remove(IccCardExtension extension) {
        mSubExtensionList.remove(extension);
    }

    @Override
    public Drawable getIconDrawableBySimInfoRecord(SimInfoRecord simInfo, String commd) {
        for (IccCardExtension subExtension : mSubExtensionList) {
            if (TextUtils.equals(commd, subExtension.getCommond())) {
                return subExtension.getIconDrawableBySimInfoRecord(simInfo, commd);
            }
        }
        return super.getIconDrawableBySimInfoRecord(simInfo, commd);
    }

    @Override
    public String getIccPhotoUriString(Bundle args, String commd) {
        for (IccCardExtension subExtension : mSubExtensionList) {
            if (TextUtils.equals(commd, subExtension.getCommond())) {
                return subExtension.getIccPhotoUriString(args, commd);
            }
        }
        return super.getIccPhotoUriString(args, commd);
    }

    @Override
    public long getIccPhotoId(Bundle args, String commd) {
        for (IccCardExtension subExtension : mSubExtensionList) {
            if (TextUtils.equals(commd, subExtension.getCommond())) {
                return subExtension.getIccPhotoId(args, commd);
            }
        }
        return super.getIccPhotoId(args, commd);
    }

    @Override
    public Drawable getIccPhotoDrawable(Bundle args, String commd) {
        for (IccCardExtension subExtension : mSubExtensionList) {
            if (TextUtils.equals(commd, subExtension.getCommond())) {
                return subExtension.getIccPhotoDrawable(args, commd);
            }
        }
        return super.getIccPhotoDrawable(args, commd);
    }

}
