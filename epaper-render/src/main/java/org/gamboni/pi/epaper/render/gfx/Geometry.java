/**
 * 
 */
package org.gamboni.pi.epaper.render.gfx;

import java.awt.geom.Point2D;

/**
 * @author tendays
 *
 */
public abstract class Geometry {
	public static Point2D shift(Point2D p, int dx, int dy) {
		return new Point2D.Double(p.getX() + dx, p.getY() + dy);
	}
}
