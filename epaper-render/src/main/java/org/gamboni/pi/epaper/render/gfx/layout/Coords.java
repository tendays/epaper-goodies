/**
 * 
 */
package org.gamboni.pi.epaper.render.gfx.layout;

import java.awt.geom.Point2D;

/**
 * @author tendays
 *
 */
public interface Coords {

	public double getX();
	public double getY();
	
	public double getRadius();
	public double getAngle();	
	
	Point2D toPoint();

	default Coords plus(Coords that) {
		return this.plus(that.getX(), that.getY());
	}
	
	default Coords plus(double x, double y) {
		return new RectCoords(this.getX() + x, this.getY() + y);
	}
	
}
