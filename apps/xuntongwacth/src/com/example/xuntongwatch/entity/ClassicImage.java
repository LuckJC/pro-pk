package com.example.xuntongwatch.entity;

import android.graphics.Bitmap;

public class ClassicImage extends GridViewItemImageView {

	private Bitmap bitmap;

	public Bitmap getBitmap() {
		return bitmap;
	}

	public void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
	}

	public ClassicImage() {
		super();
	}

	public ClassicImage(Bitmap bitmap) {
		super();
		this.bitmap = bitmap;
	}

}
