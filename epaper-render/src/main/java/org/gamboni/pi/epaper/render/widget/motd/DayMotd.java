package org.gamboni.pi.epaper.render.widget.motd;

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;

public class DayMotd implements MotdWidget.Face {
	
	private final Month month;
	private final int day;
	private final String message;

	public DayMotd(Month month, int day, String message) {
		this.month = month;
		this.day = day;
		this.message = message;
	}

	@Override
	public Optional<MotdWidget.Message> getMotd() {
		if (LocalDate.now().getMonth() == this.month && LocalDate.now().getDayOfMonth() == this.day) {
			return Optional.of(new MotdWidget.Message(message, MotdWidget.Priority.DEFAULT));
		} else {
			return Optional.empty();
		}
	}
	

}
