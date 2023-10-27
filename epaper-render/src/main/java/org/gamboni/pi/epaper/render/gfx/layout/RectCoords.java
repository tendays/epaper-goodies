/**
 * 
 */
package org.gamboni.pi.epaper.render.gfx.layout;

import java.awt.geom.Point2D;

/**
 * @author tendays
 *
 */
public class RectCoords implements Coords {
	public final double x, y;

	public RectCoords(double x, double y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public Point2D toPoint() {
		return new Point2D.Double(x, y);
	}

	@Override
	public double getX() {
		return x;
	}

	@Override
	public double getY() {
		return y;
	}

	@Override
	public double getRadius() {
		return Math.hypot(x, y);
	}

	@Override
	public double getAngle() {
		return Math.atan2(y, x);
	}
}
