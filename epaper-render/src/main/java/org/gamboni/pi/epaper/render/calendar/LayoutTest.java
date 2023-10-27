/**
 * 
 */
package org.gamboni.pi.epaper.render.calendar;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

import javax.imageio.ImageIO;

import org.gamboni.pi.epaper.render.gfx.Graphics2dDrawable;
import org.gamboni.pi.epaper.render.gfx.ImageLoader;
import org.gamboni.pi.epaper.render.gfx.layout.ElementCollection;
import org.gamboni.pi.epaper.render.gfx.layout.LayoutElement;

/**
 * @author tendays
 *
 */
public class LayoutTest {
	@SuppressWarnings("unchecked")
	public static void main(String[] a) {
		int width = 640;
		int height = 348;
		try (ObjectInputStream in = new ObjectInputStream(new FileInputStream("elements-dump"))) {
			ElementCollection elts = new ElementCollection(
					new Rectangle2D.Double(
							-height/2, -height/2, height, height));
			((List<LayoutElement>)in.readObject()).forEach(elts::add);

			BufferedImage image = new BufferedImage(height, height, BufferedImage.TYPE_INT_RGB);
			Graphics2dDrawable d = new Graphics2dDrawable(new ImageLoader(), width, height, image.createGraphics()) {
				
				@Override
				public void save() throws IOException {
					ImageIO.write(image, "png", new File("layout-test.png"));
				}
			};
			
			elts.render(d);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
