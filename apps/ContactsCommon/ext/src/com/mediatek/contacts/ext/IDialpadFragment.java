package com.mediatek.contacts.ext;

import android.content.Intent;

public interface IDialpadFragment {

    public void doCallOptionHandle(Intent intent);
    public void handleDialButtonClickWithEmptyDigits();
}
