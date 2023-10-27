/**
 * 
 */
package org.gamboni.pi.epaper.render.gfx;

import java.awt.Color;

import com.google.common.base.Splitter;

/**
 * @author tendays
 *
 */
public abstract class FixedWidthWriter {
	protected final Drawable graphics;
	private final Metrics metrics;
	protected final int left;
	protected final int top;
	private final int width;
	protected final int lineHeight;
	
	public FixedWidthWriter(Drawable graphics, int left, int top, int width) {
		this.graphics = graphics;
		this.metrics = graphics.getFontMetrics();
		this.width = width;
		this.lineHeight = (int)metrics.getHeight();
		this.left = left;
		this.top = top;
	}
	
	public void print(String text, Color color) {
		printWithBorder(text, 0, 0, color);
	}
	
	protected interface Border {
		void printBorder(int width);
	}
	
	public void printWithBorder(String text, int border, int padding, Color color) {
		final int totalSpace = border + padding;
		int maxWidth = totalSpace;
		Border borderTop = startBorder(border, padding);
		for (String line : Splitter.on('\n').split(text)) {
			final StringBuilder currentLineText = new StringBuilder();
			int currentLineWidth = totalSpace; // corresponds to the width of currentLineText
			StringBuilder nextSpaces = new StringBuilder();
			for (String word : Splitter.on(' ').split(line)) {
				if (!word.isEmpty()) {
					String prefixedWord = nextSpaces.toString() + word;
					nextSpaces.setLength(0);
					int wordWidth = (int) metrics.getStringBounds(prefixedWord).getWidth();
					if (currentLineWidth + wordWidth > width - totalSpace) {
						printLine(currentLineText.toString(), totalSpace, color);
						currentLineText.setLength(0);
						if (currentLineWidth > maxWidth) {
							maxWidth = currentLineWidth;
						}
						currentLineWidth = totalSpace;
					}
					currentLineText.append(prefixedWord);
					currentLineWidth += wordWidth;
				}
				nextSpaces.append(' ');
			}
			printLine(currentLineText.toString(), totalSpace, color);

			if (currentLineWidth > maxWidth) {
				maxWidth = currentLineWidth;
			}
		}
		borderTop.printBorder(maxWidth + totalSpace);
	}
	
	protected abstract Border startBorder(int border, int padding);

	protected abstract void printLine(String string, int x, Color color);

	public void newLine() {
		print("", Color.BLACK);
	}
	
	protected void drawRectangle(int border, int left, int top, int right, int bottom) {
		int width = right - left - border;
		int height = bottom - top - border;
		
		graphics.fillRect(left, top, width, border);
		graphics.fillRect(right - border, top, border, height);
		graphics.fillRect(left + border, bottom - border, width, border);
		graphics.fillRect(left, top + border, border, height);
	}
}
