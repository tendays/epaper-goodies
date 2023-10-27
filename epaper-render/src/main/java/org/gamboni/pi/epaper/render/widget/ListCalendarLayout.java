/**
 * 
 */
package org.gamboni.pi.epaper.render.widget;

import java.awt.Color;
import java.awt.Font;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.gamboni.pi.epaper.render.gfx.Drawable;
import org.gamboni.pi.epaper.render.calendar.EventInfo;

/**
 * @author tendays
 *
 */
public class ListCalendarLayout implements CalendarLayout {
    @Override
	public LocalDate getMinDate() {
		return LocalDate.now();
	}
	@Override
	public LocalDate getMaxDate() {
		return LocalDate.now().plusDays(7);
	}
	private static final int FONT_HEIGHT = 10;
    private static final int DAY_FONT_HEIGHT = 15;
	@Override
	public void render(Drawable g, int width, int height, List<EventInfo> data) {
    	int y = 0;
    	LocalDate day = null;
    	for (EventInfo e : data) {
    		LocalDate eventDay = LocalDate.from(e.from);
			if (day == null || eventDay.compareTo(day) > 0) {
				day = eventDay;
	    		g.setColor(Color.RED);
	    		g.setFont(new Font(Font.SERIF, Font.BOLD, DAY_FONT_HEIGHT));
	    		if (y != 0) {
	    			y += 2; // padding
	    		}
	    		y += DAY_FONT_HEIGHT;
    			LocalDate today = LocalDate.now();
				if (eventDay.compareTo(today) <= 0) {
    				day = today;
    				g.drawString("Aujourd'hui", 0, y);
    			} else if (eventDay.equals(today.plusDays(1))) {
    				g.drawString("Demain", 0, y);
    			} else if (eventDay.isBefore(today.plusDays(7))) {
    				// week day
    				g.drawString(DateTimeFormatter.ofPattern("EEEE").format(eventDay), 0, y);
    			} else {
    				// day and month
    				g.drawString(DateTimeFormatter.ofPattern("d MMMM").format(e.from), 0, y);
    			}
    		}

			g.setColor(Color.BLACK);
	    	g.setFont(new Font(Font.SERIF, Font.PLAIN, FONT_HEIGHT));
    		if (e.hasTime && !e.from.isBefore(ZonedDateTime.now())) {
    			y += FONT_HEIGHT;
    			g.drawString(DateTimeFormatter.ISO_LOCAL_TIME.format(e.from),
    					0, y);
    		}
    		y += FONT_HEIGHT;
    		g.drawString(e.text, 0, y);
    		if (y > height) { break; }
    	}

	}
}
