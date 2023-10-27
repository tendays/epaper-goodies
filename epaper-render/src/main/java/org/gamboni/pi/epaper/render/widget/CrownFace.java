/**
 * 
 */
package org.gamboni.pi.epaper.render.widget;

import java.time.LocalDate;

import org.gamboni.pi.epaper.render.gfx.Drawable;
import org.gamboni.pi.epaper.render.gfx.layout.ElementCollection;

/** Interface implemented by widgets rendering stuff on the clock's crown (which shows the next seven days).
 *
 * @author tendays
 */
public interface CrownFace {
	void renderCrown(Drawable g, BigClockWidget.ClockCoordinates coords, LocalDate date, ElementCollection elements);
}
