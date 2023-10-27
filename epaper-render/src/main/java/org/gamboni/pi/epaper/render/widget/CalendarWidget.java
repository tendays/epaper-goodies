/**
 * 
 */
package org.gamboni.pi.epaper.render.widget;
import java.awt.Color;
import java.awt.Font;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.gamboni.pi.epaper.render.Format;
import org.gamboni.pi.epaper.render.gfx.Drawable;
import org.gamboni.pi.epaper.render.gfx.layout.ElementCollection;
import org.gamboni.pi.epaper.render.gfx.layout.TextElement;
import org.gamboni.pi.epaper.render.calendar.CalendarLoader;
import org.gamboni.pi.epaper.render.calendar.EventInfo;
import org.gamboni.pi.epaper.render.widget.motd.MotdWidget;
import org.gamboni.pi.epaper.render.widget.motd.MotdWidget.Message;

import com.google.common.collect.ImmutableList;

/**
 * @author tendays
 *
 */
public class CalendarWidget implements Widget, ClockFace, CrownFace, MotdWidget.Face {
    
    private final ImmutableList<CalendarLoader> calendars;
    
	private List<EventInfo> data;
	private final CalendarLayout layout;

    public CalendarWidget(CalendarLoader... calendars) {
		this(4, ImmutableList.copyOf(calendars));
	}

    public CalendarWidget(int rows, List<CalendarLoader> calendars) {
		this.calendars = ImmutableList.copyOf(calendars);
		this.layout = new MonthCalendarLayout(MonthCalendarLayout.nextWeeks(rows));
	}

	@Override
	public void render(Drawable g, int width, int height) {
		layout.render(g, width, height, getData(layout.getMinDate(), layout.getMaxDate().plusDays(1)));
	}

    private List<EventInfo> getData(LocalDate from, LocalDate to) {
		if (this.data == null) {
			this.data = new ArrayList<>();
			for (CalendarLoader loader : this.calendars) {
				try {
					this.data.addAll(loader.loadData(from, to));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			this.data.sort(EventInfo::compareTo);
		}
		return this.data;
	}
	
	class Current {
		final StringBuilder eventText;
		final ZonedDateTime eventTime;
		
		Current(ZonedDateTime currentEventTime) {
			this.eventText = new StringBuilder();
			this.eventTime = currentEventTime;
		}
		
		TextElement toElement(TextElement.Placement placement, TextElement.Wa parameters, int fontSize) {
			String text;
			if (eventTime == null) {
				text = eventText.toString();
			} else {
				String prefix = Format.TIME_PATTERN.format(eventTime);
				if (eventText.toString().contains("\n")) {
					text = prefix + "\n" + eventText;
				} else {
					text = prefix +" "+ eventText;
				}
			}
			return new TextElement(
					text,
					placement,
					new Font(Font.SANS_SERIF, Font.PLAIN, fontSize),
					Color.BLACK,
					parameters
					.width(20, 100)
					.anchor(0, 4));
		}
		
		public void add(String text) {
			if (eventText.length() > 0) {
				eventText.append('\n');
			}
			eventText.append(text);
		}
	}

	@Override
	public void renderRadial(Drawable g, BigClockWidget.ClockCoordinates coords, ElementCollection elements) {
		class CurrentTime extends Current {

			CurrentTime(ZonedDateTime currentEventTime) {
				super(currentEventTime);
			}
			
			void addElement() {
				elements.add(this.toElement(TextElement.Placement.EMPTY_MARKER,
						TextElement.Parameters.radius(
								coords.convertRadius(0),
								coords.convertRadius(0.6))
						.direction(coords.convertHours(this.eventTime)),
						10));
			}
		}
		CurrentTime current = null;
		
		for (EventInfo e : getData(LocalDate.now(), LocalDate.now().plusDays(8))) {
			if (e.hasTime && coords.areHoursVisible(e.from)) {
				if (current == null || !e.from.equals(current.eventTime)) {
					if (current != null) {
						current.addElement();
					}
					current = new CurrentTime(e.from);
				}
				current.add(e.text);
			}
		}
		if (current != null) {
			current.addElement();
		}
	}
	
	@Override
	public void renderCrown(Drawable g, BigClockWidget.ClockCoordinates coords, LocalDate date, ElementCollection elements) {
		
		class CurrentDay extends Current {

			CurrentDay(ZonedDateTime currentEventTime) {
				super(currentEventTime);
			}
			
			void addElement() {
				elements.add(this.toElement(TextElement.Placement.CENTRED,
						TextElement.Parameters.radius(
								coords.convertRadius(0),
								coords.convertRadius(1))
						.direction(
								coords.convertDays(date.atTime(2, 0).atZone(ZoneId.systemDefault())),
								coords.convertDays(date.atTime(22, 0).atZone(ZoneId.systemDefault()))),
						9));
			}
		}
		
		CurrentDay current = null;
		
		for (EventInfo e : getData(LocalDate.now(), LocalDate.now().plusDays(8))) {
			if (e.from.toLocalDate().equals(date)) {
				if (current == null || 
						(e.hasTime ? !e.from.equals(current.eventTime) : (current.eventTime != null))) {
					if (current != null) {
						current.addElement();
					}
					current = new CurrentDay(e.hasTime ? e.from : null);
				}
				current.add(e.text);
			}
		}

		if (current != null) {
			current.addElement();
		}
	}

	@Override
	public Optional<Message> getMotd() {
		return getData(LocalDate.now(), LocalDate.now().plusDays(1)).stream()
		.filter(e -> !e.hasTime && e.from.toLocalDate().equals(LocalDate.now()))
		.findFirst()
		.map(e -> new MotdWidget.Message(e.text, MotdWidget.Priority.DEFAULT));
	}
}