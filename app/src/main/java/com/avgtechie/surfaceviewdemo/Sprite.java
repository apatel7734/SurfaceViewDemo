package com.avgtechie.surfaceviewdemo;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

public class Sprite {

	private float x;
	private float y;

	private int screenWidth;
	private int screenHeight;

	private Bitmap image;
	private Bitmap shadow;

	private Rect imageBounds;

	public Sprite(int screenWidth, int screenHeight) {
		this.x = 0;
		this.y = 0;
		this.screenWidth = screenWidth;
		this.screenHeight = screenHeight;

	}

	public void init(Bitmap image, Bitmap shadow) {
		this.image = image;
		this.shadow = shadow;
		imageBounds = new Rect(0, 0, image.getWidth(), image.getHeight());
		setX(screenWidth / 2 - getImageRect().centerX());
		setY(screenHeight / 2 - getImageRect().centerY());

	}

	public Rect getImageRect() {
		return imageBounds;
	}

	public Rect getScreenRect() {
		Rect rect = new Rect((int) x, (int) y, (int) x + getImageRect().width(), (int) y + getImageRect().height());
		return rect;
	}

	public void draw(Canvas canvas) {
		canvas.drawBitmap(shadow, x, y, null);
		canvas.drawBitmap(image, x, y, null);
	}

	public float getX() {
		return x;
	}

	public void setX(float x) {
		this.x = x;
	}

	public float getY() {
		return y;
	}

	public void setY(float y) {
		this.y = y;
	}

	public int getScreenWidth() {
		return screenWidth;
	}

	public void setScreenWidth(int screenWidth) {
		this.screenWidth = screenWidth;
	}

	public int getScreenHeight() {
		return screenHeight;
	}

	public void setScreenHeight(int screenHeight) {
		this.screenHeight = screenHeight;
	}

}
