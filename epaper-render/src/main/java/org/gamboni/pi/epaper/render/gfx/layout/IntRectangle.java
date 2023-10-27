/**
 * 
 */
package org.gamboni.pi.epaper.render.gfx.layout;

/**
 * @author tendays
 *
 */
public class IntRectangle {
	public final int left,top,right,bottom;

	public IntRectangle(int x, int y, int w, int h) {
		this.left = x;
		this.top = y;
		this.right = w;
		this.bottom = h;
	}
}
