/**
 * 
 */
package org.gamboni.pi.epaper.render.gfx.layout;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * @author tendays
 *
 */
public class Surface {

	private final ImmutableSet<Rectangle2D> rectangles;
	private final Rectangle2D boundingBox;

	public Surface(Set<Rectangle2D> rectangles) {
		this.rectangles = ImmutableSet.copyOf(rectangles);
		Rectangle2D boundingBox = null;
		for (Rectangle2D r : rectangles) {
			if (boundingBox == null) {
				boundingBox = r;
			} else {
				boundingBox = boundingBox.createUnion(r);
			}
		}
		this.boundingBox = boundingBox;
	}

	public boolean isEmpty() {
		return (boundingBox == null);
	}

	public boolean intersects(Surface that) {
		if (this.isEmpty() || that.isEmpty()) { return false; }

		if (this.getBoundingBox().intersects(that.getBoundingBox()))
			for (Rectangle2D a : this.rectangles)
				for (Rectangle2D b : that.rectangles)
					if (a.intersects(b)) {
						return true;
					}
		
		return false;
	}
	
	public Point2D getCentre() {
		return new Point2D.Double(
				getBoundingBox().getCenterX(),
				getBoundingBox().getCenterY());
	}

	public double getL() {
		return getBoundingBox().getMinX();
	}
	public double getR() {
		return getBoundingBox().getMaxX();
	}
	public double getT() {
		return getBoundingBox().getMinY();
	}
	public double getB() {
		return getBoundingBox().getMaxY();
	}

	public Rectangle2D getBoundingBox() {
		return boundingBox;
	}
}
