/**
 * 
 */
package org.gamboni.pi.epaper.render.widget;

import org.gamboni.pi.epaper.render.gfx.Drawable;
import org.gamboni.pi.epaper.render.gfx.layout.ElementCollection;

/**
 * @author tendays
 *
 */
public interface ClockFace {

	void renderRadial(Drawable g, BigClockWidget.ClockCoordinates coords, ElementCollection elements);

}
