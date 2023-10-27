/**
 * 
 */
package org.gamboni.pi.epaper.render.gfx;

import java.awt.Color;

/**
 * @author tendays
 *
 */
public class StaticFixedWidthWriter extends FixedWidthWriter {
	/** relative to top */
	private int y;

	public StaticFixedWidthWriter(Drawable graphics, int left, int top, int width) {
		super(graphics, left, top, width);
	}

	@Override
	protected void printLine(String string, int x, Color color) {
		graphics.setColor(color);
		graphics.drawString(string, x + left, y + top);
		y += lineHeight;
	}

	@Override
	protected Border startBorder(int border, int padding) {
		int boxTop = y;
		y += border + padding;
		return (int width) -> {
			y += border + padding;
			drawRectangle(border, left, boxTop, width, y);
		};
	}
}
