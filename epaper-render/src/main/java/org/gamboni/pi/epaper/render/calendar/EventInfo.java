package org.gamboni.pi.epaper.render.calendar;

import java.awt.Color;
import java.io.Serializable;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Objects;

public class EventInfo implements Comparable<EventInfo>, Serializable {
	private static final long serialVersionUID = 1L;
	
	public final String text;
	public final ZonedDateTime from;
	public final ZonedDateTime to;
	public final boolean hasTime;
	public final Color color;
	public final URL image;

	public EventInfo(String text, ZonedDateTime from, ZonedDateTime to, boolean hasTime, Color color) {
		this(text, from, to, hasTime, color, null);
	}

	public EventInfo(String text, ZonedDateTime from, ZonedDateTime to, boolean hasTime, Color color, URL image) {
		this.from = Objects.requireNonNull(from);
		this.to = to;
		this.text = text;
		this.hasTime = hasTime;
		this.color = color;
		this.image = image;
	}

	@Override
	public int compareTo(EventInfo that) {
		try {
		return this.from.compareTo(that.from);
		} catch (NullPointerException npe) {
			System.out.println("compareTo NPE on "+ this.text);
			throw npe;
		}
	}
	
	public String toString() {
		return (hasTime ? from +" – "+ to : from.toLocalDate() +" – "+ to.toLocalDate()) +": "+ text +
				(image == null ? "" : ("<"+ image +">"));
	}
}