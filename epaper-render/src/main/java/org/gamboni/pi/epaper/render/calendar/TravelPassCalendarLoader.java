/**
 * 
 */
package org.gamboni.pi.epaper.render.calendar;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.io.CharStreams;

/** A {@link CalendarLoader} providing reminders to renew my travel pass.
 *
 * @author tendays
 */
public class TravelPassCalendarLoader implements CalendarLoader {
	
	private final String url;
	private final String name;
	
	public TravelPassCalendarLoader(String url, String name) {
		this.url = url;
		this.name = name;
	}

	@Override
	public List<EventInfo> loadData(LocalDate from, LocalDate to) throws IOException {
		try (InputStreamReader r = new InputStreamReader(new URL(url).openStream())) {
			LocalDate expiration = LocalDate.parse(Iterables.getOnlyElement(CharStreams.readLines(r)), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
			int age = 0;
			while (expiration.isBefore(LocalDate.now())) {
				expiration = expiration.plusMonths(1);
				age++;
			}
			
			if (age < 5) {
			return ImmutableList.of(new EventInfo("Renouveler " + name + (age > 0 ? " ("+Strings.repeat("?", age)+")" : ""),
					toZonedDateTime(expiration),
					toZonedDateTime(expiration.plusDays(1)),
					false,
					Color.RED));
			} else {
				return ImmutableList.of();
			}
		}
	}

	protected ZonedDateTime toZonedDateTime(LocalDate expiration) {
		return expiration
		.atStartOfDay()
		.atZone(ZoneId.systemDefault());
	}


}
