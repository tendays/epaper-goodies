package org.gamboni.pi.epaper.render.gfx.layout;

import java.awt.geom.Point2D;

public class PolarCoords implements Coords {
	public static final PolarCoords ORIGIN = new PolarCoords(0, 0);
	public static final double TWO_PI = Math.PI*2;
	public final double radius;
	public final double angle;
	
	public PolarCoords(double radius, double angle) {
		this.radius = radius;
		this.angle = angle;
	}
	
	@Override
	public Point2D toPoint() {
		return new Point2D.Double(getX(), getY());
	}

	@Override
	public double getX() {
		return Math.sin(angle) * radius;
	}

	@Override
	public double getY() {
		return -Math.cos(angle) * radius;
	}

	@Override
	public double getRadius() {
		return radius;
	}

	@Override
	public double getAngle() {
		return angle;
	}
	
	
}