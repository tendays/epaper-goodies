/**
 * 
 */
package org.gamboni.pi.epaper.render.gfx;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * @author tendays
 *
 */
public class ThreeColourGraphics extends Graphics2dDrawable  {
	private final BufferedImage red;
	private final BufferedImage black;
	private final BufferedImage preview;
	
	private final Graphics2D redGraphics;
	private final Graphics2D blackGraphics;
	private final Graphics2D previewGraphics;

	public ThreeColourGraphics(ImageLoader images, int width, int height) {
		this(images, newBufferedImage(width, height), newBufferedImage(width, height), newBufferedImage(width, height));
	}
	
	private ThreeColourGraphics(ImageLoader images, BufferedImage red, BufferedImage black, BufferedImage preview) {
		this(images,
				red, black, preview,
				red.createGraphics(),
				black.createGraphics(),
				preview.createGraphics());
	}
	
	private ThreeColourGraphics(ImageLoader images, BufferedImage red, BufferedImage black, BufferedImage preview,
			Graphics2D redGraphics, Graphics2D blackGraphics, Graphics2D previewGraphics) {
		super(images, preview.getWidth(), preview.getHeight(), redGraphics, blackGraphics, previewGraphics);
		this.red = red;
		this.black = black;
		this.preview = preview;
		this.redGraphics = redGraphics;
		this.blackGraphics = blackGraphics;
		this.previewGraphics = previewGraphics;
	}
	
	@Override
	public void save() throws IOException {
		ImageIO.write(black, "png", new File("black.png"));
		ImageIO.write(red, "png", new File("red.png"));
		ImageIO.write(preview, "png", new File("preview.png"));
	}

	@Override
	public void setColor(Color color) {
		if (color.equals(Color.BLACK)) {
			blackGraphics.setColor(Color.BLACK);
			redGraphics.setColor(Color.WHITE);
		} else if (color.equals(Color.RED)) {
			blackGraphics.setColor(Color.BLACK);
			redGraphics.setColor(Color.BLACK);
		} else if (color.equals(Color.WHITE)) {
			blackGraphics.setColor(Color.WHITE);
			redGraphics.setColor(Color.WHITE);
		} else {
			throw new IllegalArgumentException("Unsupported color "+ color);
		}
		
		previewGraphics.setColor(color);
	}
}
