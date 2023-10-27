/**
 * 
 */
package org.gamboni.pi.epaper.render.gfx;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.util.List;

import org.gamboni.pi.epaper.render.gfx.layout.Coords;
import org.gamboni.pi.epaper.render.widget.Widget;

/** Interface similar to Graphics2D, with many methods missing and some methods added.
 *
 * @author tendays
 */
public interface Drawable {

	void save() throws IOException;

	void render(Widget wid, int left, int top, int right, int bottom);

	void setColor(Color color);

	void drawString(String text, int x, int y);

	void fillRect(int x, int y, int w, int h);

	Font getFont();
	void setFont(Font font);

	void drawPixel(int x, int y);

	void drawLine(int x1, int y1, int x2, int y2);

	void drawCircle(int x, int y, int r);

	void drawCircle(Coords centre, int r);

	void fillCircle(Coords centre, int r);

	void drawString(String string, Coords position);

	void fillPolygon(List<Coords> points);

	void setStroke(int width);

	Metrics getFontMetrics();

	Metrics getFontMetrics(Font font);
	
	/** Set the given coordinate to be the new (0, 0) point during the execution of the given Runnable. */
	void setOrigin(int x, int y, Runnable r);

	void drawLine(Coords from, Coords to);

	enum Anchor {
		CENTRE {
			@Override
			public int getXOffset(int width) {
				return -width/2;
			}

			@Override
			public int getYOffset(int height) {
				return -height/2;
			}
		}, RIGHT {
			@Override
			public int getXOffset(int width) {
				return -width;
			}

			@Override
			public int getYOffset(int height) {
				return -height/2;
			}
		};
		public abstract int getXOffset(int width);
		public abstract int getYOffset(int height);
	}

	enum ImageStyle {
		BW_EDGE, OPAQUE, WHITE_TRANSPARENT, LIGHT
	}

	void drawIcon(String icon, Coords coords, int maxHeight, Anchor anchor, ImageStyle style);

	default void debugBox(Coords topLeft, Coords bottomRight) {}

}
