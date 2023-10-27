/**
 * 
 */
package org.gamboni.pi.epaper.render.gfx.layout;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.gamboni.pi.epaper.render.gfx.Drawable;
import org.gamboni.pi.epaper.render.gfx.Metrics;
import org.gamboni.pi.epaper.render.widget.Widget;

/**
 * @author tendays
 *
 */
public class CoverageRecorder implements Drawable {
	
	private final Drawable graphics;
	private final Set<Rectangle2D> rectangles = new HashSet<>();
	private Font font;
	
	public CoverageRecorder(Drawable drawable) {
		this.graphics = drawable;
		
	}

	public Surface getCoverage() {
		return new Surface(rectangles);
	}

	@Override
	public void save() throws IOException {
	}

	@Override
	public void render(Widget wid, int left, int top, int right, int bottom) {
	}

	@Override
	public void setColor(Color color) {
	}

	@Override
	public void drawString(String text, int x, int y) {
	}

	@Override
	public void fillRect(int x, int y, int w, int h) {
		rectangles.add(new Rectangle2D.Double(x, y, w, h));
	}

	@Override
	public void setFont(Font font) {
		this.font = font;
	}

	@Override
	public void drawPixel(int x, int y) {
	}

	@Override
	public void drawLine(int x1, int y1, int x2, int y2) {
	}

	@Override
	public void drawCircle(int x, int y, int r) {
	}

	@Override
	public void drawCircle(Coords centre, int r) {
	}

	@Override
	public void fillCircle(Coords centre, int r) {
	}

	@Override
	public void drawLine(Coords from, Coords to) {
	}

	@Override
	public void drawString(String string, Coords position) {
		Rectangle2D rect = getFontMetrics().getStringBounds(string);
		rectangles.add(new Rectangle2D.Double(
				position.getX() + rect.getMinX(),
				position.getY() + rect.getMinY(),
				rect.getWidth(),
				rect.getHeight()));
	}
	
	@Override
	public void drawIcon(String icon, Coords position, int maxHeight, Anchor anchor, ImageStyle style) {
		if (icon.isEmpty()) { return; }
		Metrics.Size rect = graphics.getFontMetrics().getIconBounds(icon);
		double scale =  (rect.height > maxHeight) ? ((double)rect.height) / maxHeight : 1;
		
		rectangles.add(new Rectangle2D.Double(
				position.getX() + anchor.getXOffset((int) (rect.width * scale)),
				position.getY() + anchor.getYOffset((int) (rect.height * scale)),
				rect.width * scale,
				rect.height * scale));
	}

	@Override
	public void fillPolygon(List<Coords> points) {
	}

	@Override
	public void setStroke(int width) {
	}

	@Override
	public Metrics getFontMetrics() {
		return getFontMetrics(this.getFont());
	}

	@Override
	public Font getFont() {
		if (this.font == null) {
			return graphics.getFont();
		} else {
			return this.font;
		}
	}

	@Override
	public Metrics getFontMetrics(Font font) {
		return graphics.getFontMetrics(font);
	}

	@Override
	public void setOrigin(int x, int y, Runnable r) {
		throw new UnsupportedOperationException();
	}
}
