/**
 * 
 */
package org.gamboni.pi.epaper.render.gfx.layout;

/**
 * @author tendays
 *
 */
public enum Corner {
	TOP_RIGHT(1, -1),
	TOP_LEFT(-1, -1),
	BOTTOM_LEFT(-1, 1),
	BOTTOM_RIGHT(1, 1);
	
	public final int x, y;
	
	private Corner(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	/** Suppose the given force is applied to a Rectangle. Then return the corner which receives pressure. */
	public static Corner ofForce(Force f) {
		if (f.x < 0) {
			return (f.y < 0) ? BOTTOM_RIGHT : TOP_RIGHT;
		} else {
			return (f.y < 0) ? BOTTOM_LEFT : TOP_LEFT;
		}
	}
}
