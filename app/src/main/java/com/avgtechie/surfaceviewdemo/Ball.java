package com.avgtechie.surfaceviewdemo;

import java.util.Random;

import android.graphics.Rect;

public class Ball extends Sprite {

	private final float speedX = 0.3f;
	private final float speedY = 0.3f;

	private int directionX;
	private int directionY;

	public Ball(int screenWidth, int screenHeight) {
		super(screenWidth, screenHeight);
		setDirectionX();
		setDirectionY();

	}

	public void update(long elapsed) {
		float x = getX();
		float y = getY();

		Rect screenRect = getScreenRect();

		if (screenRect.left <= 0) {
			directionX = 1;
		} else if (screenRect.right >= getScreenWidth()) {
			directionX = -1;
		} else if (screenRect.top < 0) {
			directionY = 1;
		} else if (screenRect.bottom >= getScreenHeight()) {
			directionY = -1;
		}

		x += directionX * speedX * elapsed;
		y += directionY * speedY * elapsed;

		setX(x);
		setY(y);
	}

	public void initPosition() {
		setX(getScreenWidth() / 2 - getImageRect().centerX());
		setY(getScreenHeight() / 2 - getImageRect().centerY());
	}

	public int getDirectionX() {
		return directionX;
	}

	public void setDirectionX() {
		Random rand = new Random();
		this.directionX = rand.nextInt(2) * 2 - 1;
	}

	public int getDirectionY() {
		return directionY;
	}

	public void setDirectionY() {

		Random rand = new Random();
		this.directionY = rand.nextInt(2) * 2 - 1;
	}

	public void moveRight() {
		directionX = 1;
	}

	public void moveLeft() {
		directionX = -1;
	}

}
