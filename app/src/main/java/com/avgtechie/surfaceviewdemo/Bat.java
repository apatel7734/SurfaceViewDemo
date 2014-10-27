package com.avgtechie.surfaceviewdemo;

import java.util.Random;

import android.graphics.Bitmap;

public class Bat extends Sprite {

	private static final int margin = 20;
	private int directionY;
	private float speedY = 0.5f;

	public enum Position {
		LEFT, RIGHT;
	}

	private Position position;
	Random random = new Random();

	public Bat(int screenWidth, int screenHeight, Position position) {
		super(screenWidth, screenHeight);
		this.position = position;
	}

	@Override
	public void init(Bitmap image, Bitmap shadow) {
		super.init(image, shadow);
		initPosition();
		if (position == Position.LEFT) {
			setX(margin);
		} else if (position == Position.RIGHT) {
			setX((getScreenWidth() - margin) - getImageRect().centerX());
		}
		this.directionY = getRandomDirectionY();

	}

	public void initPosition() {
		setY(getScreenHeight() / 2 - getImageRect().centerY());
	}

	public void setPosition(float y) {

		setY(y - getImageRect().centerY());
	}

	// Only computer can use this method to update
	public void update(long elapsed, Ball ball) {

		float y = getY();
		int decision = random.nextInt(20);
		if (decision == 0) {
			directionY = 0;
		} else if (decision == 1) {
			directionY = getRandomDirectionY();
		} else if (decision < 8) {
			if (ball.getScreenRect().centerY() < getScreenRect().centerY()) {
				directionY = -1;
			} else {
				directionY = 1;
			}

		}

		if (getScreenRect().top <= 0) {
			directionY = 1;
		} else if (getScreenRect().bottom >= getScreenHeight()) {
			directionY = -1;
		}

		y += elapsed * speedY * directionY;
		setY(y);

	}

	public int getRandomDirectionY() {
		Random rand = new Random();
		int r = rand.nextInt(2);
		return r * 2 - 1;
	}
}
