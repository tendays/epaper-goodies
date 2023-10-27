/**
 * 
 */
package org.gamboni.pi.epaper.render.widget;

import java.awt.Color;
import java.awt.Font;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.gamboni.pi.epaper.render.gfx.Drawable;
import org.gamboni.pi.epaper.render.gfx.layout.ElementCollection;
import org.gamboni.pi.epaper.render.gfx.layout.TextElement;

import net.time4j.PlainDate;
import net.time4j.calendar.ChineseCalendar;

/**
 * @author tendays
 *
 */
public class ChineseCalendarWidget implements ClockFace, CrownFace {

	@Override
	public void renderRadial(Drawable g, BigClockWidget.ClockCoordinates coords, ElementCollection elements) {
		// Nothing on clock face
	}

	@Override
	public void renderCrown(Drawable g, BigClockWidget.ClockCoordinates coords, LocalDate date, ElementCollection elements) {
		ZonedDateTime dayStart = date.atStartOfDay(ZoneId.systemDefault());
		elements.add(new TextElement(format(date), TextElement.Placement.CENTRED,
				new Font(Font.SANS_SERIF, Font.PLAIN, 10),
				Color.RED,
				TextElement.Parameters.radius(coords.convertRadius(0.2), coords.convertRadius(0.8))
				.direction(coords.convertDays(dayStart.plusHours(4)), coords.convertDays(dayStart.plusHours(15)))
				.width(8, 50)
				.anchor(0, 0)));
	}

	private String format(LocalDate date) {
		ChineseCalendar cc = PlainDate.from(date).transform(ChineseCalendar.axis());

		return cc.getDayOfMonth() + "/" + cc.getMonth().getNumber();
	}
}
