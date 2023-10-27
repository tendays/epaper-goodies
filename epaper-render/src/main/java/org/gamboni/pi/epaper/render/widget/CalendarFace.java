/**
 * 
 */
package org.gamboni.pi.epaper.render.widget;

import java.time.LocalDate;

import org.gamboni.pi.epaper.render.gfx.Drawable;

/**
 * @author tendays
 *
 */
public interface CalendarFace {

	void renderOnCalendar(Drawable g, int left, int top, int width, int height, LocalDate date);

}
