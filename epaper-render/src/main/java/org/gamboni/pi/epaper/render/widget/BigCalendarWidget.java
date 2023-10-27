/**
 * 
 */
package org.gamboni.pi.epaper.render.widget;

import java.awt.Color;
import java.awt.Font;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.gamboni.pi.epaper.render.gfx.Drawable;

/**
 * @author tendays
 *
 */
public class BigCalendarWidget extends WidgetHolder<CalendarFace> implements Widget {

	private static final int FONT_SIZE_DAY = 10;

	private static String dayName(int delta) {
		if (delta==0) {
			return "aujourd'hui";
		} else if (delta==1) {
			return "demain";
		} else {
			return DateTimeFormatter.ofPattern("EEEE").format(LocalDate.now().plusDays(delta));
		}
	}
	
	@Override
	public void render(Drawable g, int width, int height) {
		for (int i=0; i<7; i++) {
			LocalDate date = LocalDate.now().plusDays(i);
			int pageTop = height * i / 7;
			int pageHeight = height * (i+1) / 7 - pageTop;
			
			renderWidgets(face -> face.renderOnCalendar(g, 0, pageTop, width, pageHeight, date));
			
			g.setColor(Color.BLACK);
			g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, FONT_SIZE_DAY));
			g.drawString(dayName(i), 0, pageTop + FONT_SIZE_DAY);

			g.setFont(new Font(Font.SERIF, Font.PLAIN, height/7 - FONT_SIZE_DAY - 2));
			g.drawString(String.valueOf(date.getDayOfMonth()), 0, height * (i+1) / 7);
		}
		g.setColor(Color.BLACK);
		for (int i=1; i<7; i++) {
			int y = height * i / 7;
			g.drawLine(0, y, width, y);
		}
		
	}
}
