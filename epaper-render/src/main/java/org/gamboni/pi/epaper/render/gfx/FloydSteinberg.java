/**
 * 
 */
package org.gamboni.pi.epaper.render.gfx;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.google.common.base.Preconditions;

/**
 * @author tendays
 *
 */
public class FloydSteinberg extends Graphics2dDrawable {
	private final BufferedImage fullColour;

	public FloydSteinberg(ImageLoader images, int width, int height) {
		this(images, newBufferedImage(width, height));
	}

	private FloydSteinberg(ImageLoader images, BufferedImage fullColour) {
		super(images, fullColour.getWidth(), fullColour.getHeight(),
				fullColour.createGraphics());
		
		this.fullColour = fullColour;
	}
	
	@Override
	public void setColor(Color color) {
		int grue = (color.getBlue() + color.getGreen()) / 2;

		super.setColor(new Color(Math.max(color.getRed(), grue), grue, grue));
	}
	
	private class Errors {
		/** errors contributed by previous line */
		double[] thisLineError;
		/** errors for next line */
		double[] nextLineError = new double[width];
		
		void newLine() {
			thisLineError = nextLineError;
			nextLineError = new double[width];
		}

		public void spread(int x, double residue) {
			// Floyd-Steinberg coefficients

			if (x > 0) {
				nextLineError[x - 1] += residue * 3 / 16;
			}
			nextLineError[x] += residue * 5 / 16;
			if (x < width - 1) {
				thisLineError[x + 1] += residue * 7 / 16;
				nextLineError[x + 1] += residue / 16;
			}
		}

		public int get(int x) {
			return (int)thisLineError[x];
		}
	}

	@Override
	public void save() throws IOException {
		BufferedImage red = newBufferedImage(width, height);
		BufferedImage black = newBufferedImage(width, height);
		BufferedImage preview = newBufferedImage(width, height);
		
		Errors redErrors = new Errors();
		Errors gbErrors = new Errors();
		
		for (int y = 0; y < height; y++) {
			redErrors.newLine();
			gbErrors.newLine();
			for (int x = 0; x < width; x++) {
				/* Select final red/black/white colour for this pixel */
				int rgbIn = fullColour.getRGB(x, y);
				int r = ((rgbIn >> 16) & 0xFF) + redErrors.get(x);
				int gb = (rgbIn & 0xFF) + gbErrors.get(x);
				boolean redPixel; // true if pixel is red
				boolean whitePixel; // true if pixel is white
				// if both are false, pixel is black
				
				Color colourOut;
				if (r < 128) { // black
					redPixel = false;
					whitePixel = false;
					colourOut = Color.BLACK;
					
				} else if (gb < 128) { // red
					redPixel = true;
					whitePixel = false;
					colourOut = Color.RED;
					
				} else { // white
					redPixel = false;
					whitePixel = true;
					colourOut = Color.WHITE;
				}
				red.setRGB(x, y, (redPixel ? Color.BLACK : Color.WHITE).getRGB());
				black.setRGB(x, y, (whitePixel ? Color.WHITE : Color.BLACK).getRGB());
				preview.setRGB(x, y, colourOut.getRGB());
				
				redErrors.spread(x, r - (whitePixel || redPixel ? 255 : 0));
				gbErrors.spread(x, gb - (whitePixel ? 255 : 0));
			}
		}

		ImageIO.write(black, "png", new File("black.png"));
		ImageIO.write(red, "png", new File("red.png"));
		ImageIO.write(preview, "png", new File("preview.png"));
		ImageIO.write(fullColour, "png", new File("full.png"));
	}
}
