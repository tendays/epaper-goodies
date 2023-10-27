/**
 * 
 */
package org.gamboni.pi.epaper.render.widget;

import org.gamboni.pi.epaper.render.gfx.Drawable;

/**
 * @author tendays
 *
 */
public interface Widget {
	void render(Drawable graphics, int width, int height);
}
