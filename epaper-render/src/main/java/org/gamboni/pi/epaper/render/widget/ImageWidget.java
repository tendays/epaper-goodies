/**
 * 
 */
package org.gamboni.pi.epaper.render.widget;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.gamboni.pi.epaper.render.gfx.Drawable;

import com.google.common.collect.TreeMultiset;

/**
 * @author tendays
 *
 */
public class ImageWidget implements Widget {

	private final File imageFile;
	private final boolean scaleBrightness;
	
	public ImageWidget(String imagePath, boolean scaleBrightness) {
		this.imageFile = new File(imagePath);
		this.scaleBrightness = scaleBrightness;
	}
	
	private static class BrightnessDistribution {
		final int slotCount;
		/* Collect all brightnesses in an ordered multiset */
		final TreeMultiset<Integer> brightnessDistribution = TreeMultiset.create();
		final List<Integer> samples;
		
		BrightnessDistribution(int slotCount) {
			this.slotCount = slotCount;
			this.samples = new ArrayList<>(slotCount);
		}
		
		void add(int brightness) {
			brightnessDistribution.add(brightness);
		}

		void compute() {
			int counter = 0;
			for (int brightness : brightnessDistribution) {
				counter ++;
				if (counter > (samples.size()+1) * brightnessDistribution.size() / slotCount) {
					samples.add(brightness);
				}
			}
		}
	}

	@Override
	public void render(Drawable g, int width, int height) {
		try {
			BufferedImage bitmap = ImageIO.read(imageFile);
			double zoom = Math.max(
					((double)width / bitmap.getWidth()),
					((double)height / bitmap.getHeight()));

			if (scaleBrightness) { // no effect for now, just don't want to throw the code away
				int slotCount = 50;
				BrightnessDistribution distribution = new BrightnessDistribution(slotCount);
				for (int y = 0; y < height; y++)
					for (int x = 0; x < width; x++) {
						distribution.add(getBrightness(bitmap, zoom, width, height, x, y));
					}
				distribution.compute();
			}

			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int rgb = getRGB(bitmap, zoom, width, height, x, y);
					g.setColor(new Color(rgb));
					g.drawPixel(x, y);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private int getBrightness(BufferedImage bitmap, double zoom, int width, int height, int x, int y) {
		int rgb = getRGB(bitmap, zoom, width, height, x, y);
		int brightness = (rgb & 0xFF) +
				((rgb >> 8) & 0xFF) +
				((rgb >> 16) & 0xFF);
		return brightness;
	}

	public int getRGB(BufferedImage bitmap, double zoom, int width, int height, int x, int y) {
		return bitmap.getRGB(
				(int)(bitmap.getWidth()/2 + (x - width/2) / zoom),
				(int)(bitmap.getHeight()/2 + (y - height/2) / zoom));
	}
}
