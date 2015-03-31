package com.mediatek.encapsulation.android.media;

import android.media.ExifInterface;

import java.io.IOException;
import java.io.InputStream;

public class EncapsulatedExifInterface extends ExifInterface {

    public EncapsulatedExifInterface(InputStream stream) throws IOException {
        super(stream);
    }

}