/**
 * 
 */
package org.gamboni.pi.epaper.render.widget;

import java.time.LocalDate;
import java.util.List;

import org.gamboni.pi.epaper.render.gfx.Drawable;
import org.gamboni.pi.epaper.render.calendar.EventInfo;

/**
 * @author tendays
 *
 */
public interface CalendarLayout {

	void render(Drawable g, int width, int height, List<EventInfo> data);

	LocalDate getMinDate();
	LocalDate getMaxDate();

}
