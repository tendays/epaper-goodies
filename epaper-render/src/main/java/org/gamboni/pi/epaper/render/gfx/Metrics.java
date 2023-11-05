package org.gamboni.pi.epaper.render.gfx;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.function.Function;

public class Metrics {
	private final FontMetrics fontMetrics;
	private final Graphics2D g;
	private Function<String, Size> iconBounds;
	
	public static class Size {
		public final int width, height;

		public Size(int width, int height) {
			this.width = width;
			this.height = height;
		}

		public Size scale(double factor) {
			return new Size((int)(width * factor), (int)(height * factor));
		}
	}

	public Metrics(FontMetrics fontMetrics, Graphics2D g, Function<String, Size> iconBounds) {
		this.fontMetrics = fontMetrics;
		this.g = g;
		this.iconBounds = iconBounds;
	}

	public Rectangle2D getStringBounds(String text) {
		return fontMetrics.getStringBounds(text, g);
	}

	public double getHeight() {
		return fontMetrics.getHeight();
	}

	public double getAscent() {
		return fontMetrics.getAscent();
	}

	public Size getIconBounds(String icon) {
		return iconBounds.apply(icon);
	}
}
