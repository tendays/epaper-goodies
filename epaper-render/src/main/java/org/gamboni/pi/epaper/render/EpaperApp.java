/**
 * 
 */
package org.gamboni.pi.epaper.render;

import org.gamboni.pi.epaper.render.gfx.FloydSteinberg;
import org.gamboni.pi.epaper.render.gfx.ImageLoader;
import org.gamboni.pi.epaper.render.server.EpaperServer;

/**
 * @author tendays
 *
 */
public class EpaperApp {
	public static void main(String[] a) {
		try {
			if (a.length == 0) {
				System.err.println("Usage: " + EpaperApp.class.getSimpleName() + " --server|--render");
				System.exit(1);
			} else if (a[0].equals("--server")) {
				if (a.length != 2) {
					System.err.println("Usage: " + EpaperApp.class.getSimpleName() + " --server config-file");
					System.exit(1);
				}
				new EpaperServer(a[1]).run();
			} else if (a[0].equals("--render")) {
				if (a.length != 2) {
					System.err.println("Usage: " + EpaperApp.class.getSimpleName() + " --render layout-file");
					System.exit(1);
				}
				render(a[1]);
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	private static void render(String layoutFile) throws Exception {
		Layout layout = Layout.parseConfigFile(layoutFile, true);
		FloydSteinberg graphics = layout.getDimensions((w, h) -> new FloydSteinberg(new ImageLoader(), w, h));
		layout.render(graphics);
		graphics.save();
	}
}
