/**
 * 
 */
package org.gamboni.pi.epaper.render.widget;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import org.gamboni.pi.epaper.render.gfx.Drawable;
import org.gamboni.pi.epaper.render.gfx.Metrics;
import org.gamboni.pi.epaper.render.gfx.layout.RectCoords;

import com.google.common.collect.ImmutableList;

/**
 * @author tendays
 *
 */
public class ProgressWidget implements Widget {
	private final String unit, startLabel, currentLabel;
	public ProgressWidget(String unit, String start, String current) {
		this.unit = unit;
		this.startLabel = start;
		this.currentLabel = current;
	}

	@Override
	public void render(Drawable graphics, int width, int height) {
		File file = new File("/tmp/distance");
		if (!file.exists()) { return; }
		
		try (LineNumberReader reader = new LineNumberReader(new FileReader(file))) {
			String line = reader.readLine();
			int slash = line.indexOf("/");
			if (slash == -1) {
				throw new IllegalArgumentException("Invalid file content "+ line);
			}
			
			int current = Integer.parseInt(line.substring(0, slash));
			int top = Integer.parseInt(line.substring(slash+1));
			
			Metrics metrics = graphics.getFontMetrics();
			ImmutableList<RectCoords> corners = ImmutableList.of(
					new RectCoords(0, height - 3), new RectCoords(width - 1, height - 3),
					new RectCoords(width - 1, height - 1), new RectCoords(0, height - 1));
			graphics.setColor(Color.BLACK);
			for (int k=0; k<4; k++) {
				graphics.drawLine(corners.get(k), corners.get((k+1) % 4));
			}
			
			int barX = (top > 0) ? (width - 2) * (top - current) / top : 1;
			graphics.setColor(Color.RED);
			graphics.drawLine(1, height - 2, barX, height - 2);
			
			graphics.setColor(Color.BLACK);
			String topText = startLabel + ": "+ top + unit;
			Rectangle2D topBounds = metrics.getStringBounds(topText);
			int textY = (int)( height - metrics.getAscent());
			graphics.drawString(topText, 0, textY);
			
			graphics.setColor(Color.RED);
			String currentText = currentLabel +": "+ current + unit;
			Rectangle2D currentBounds = metrics.getStringBounds(currentText);
			graphics.drawString(currentText, (int)
					Math.min(width - currentBounds.getWidth(),
					Math.max(topBounds.getWidth(), barX - currentBounds.getWidth()/2)), textY);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
