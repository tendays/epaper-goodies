/**
 * 
 */
package org.gamboni.pi.epaper.render.widget;

import static java.util.stream.Collectors.toList;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.gamboni.pi.epaper.render.Format;
import org.gamboni.pi.epaper.render.gfx.Drawable;
import org.gamboni.pi.epaper.render.gfx.layout.ElementCollection;
import org.gamboni.pi.epaper.render.gfx.layout.TextElement;

import com.google.common.collect.Streams;
import com.google.gson.JsonParser;

/**
 * @author tendays
 *
 */
public class TimetableWidget implements Widget, ClockFace {
	
	private static class Place {
		final String fullName, shortName;
		Place(String fullName, String shortName) {
			this.fullName = fullName;
			this.shortName = shortName;
		}
		public String toString() {
			return fullName;
		}
		
		public static Place parse(String string) {
			string = string.trim();
			int open = string.indexOf("(");
			int close = string.indexOf(")", open);
			if (open != -1 && close > open) {
				if (close != string.length() - 1) {
					throw new IllegalArgumentException("Extraneous characters after ')' in " + string);
				}
				return new Place(string.substring(0, open).trim(),
						string.substring(open+1, close));
			} else {
				return new Place(string, string);
			}
		}
	}
	
	private static class Journey {
		public final Place from;
		public final Place to;
		public Journey(Place from, Place to) {
			this.from = from;
			this.to = to;
		}
		
		static Journey parse(String string) {
			int arrow = string.indexOf("->");
			if (arrow == -1) {
				throw new IllegalArgumentException("Arrow '->' expected");
			}
			return new Journey(Place.parse(string.substring(0, arrow)),
					Place.parse(string.substring(arrow+2)));
		}
	}
	
	public TimetableWidget(List<String> connections) {
		this.journeys = connections.stream()
				.map(Journey::parse)
				.collect(toList());
	}

	private final List<Journey> journeys;
	
	private static class Connection {
		final ZonedDateTime time;
		final String from;
		
		public Connection(ZonedDateTime time, String from) {
			this.time = time;
			this.from = from;
		}
	}
	
	@Override
	public void render(Drawable g, int width, int height) {
		int y = 10;
		g.setColor(Color.BLACK);
		for (Connection c : loadConnections()) {
			g.drawString(c.time.format(DateTimeFormatter.ofPattern("HH:mm")), 0, y);
			y += 11;
			g.drawString(c.from, 5, y);
			y += 15;
		}
	}

	private Iterable<Connection> loadConnections() {
		return () -> loadData().stream()
				.flatMap(reader -> Streams
						.stream(new JsonParser().parse(reader).getAsJsonObject().get("connections").getAsJsonArray()))
				.map(j -> j.getAsJsonObject())
				// ignore connections that start with walking (i.e. don't have a "journey")
				.filter(connection -> connection.get("sections").getAsJsonArray().get(0).getAsJsonObject().get("journey").isJsonObject())
				.map(connection -> new Connection(
						Instant.ofEpochSecond(connection.get("sections").getAsJsonArray().get(0)
								.getAsJsonObject().get("departure").getAsJsonObject().get("departureTimestamp").getAsLong())
						.atZone(ZoneId.systemDefault()),
						connection.get("from").getAsJsonObject().get("station").getAsJsonObject()
								.get("name").getAsString()))
				.sorted(Comparator.comparing(c -> c.time)).iterator();
	}
	
	private static Reader connection(Place from, Place to) throws IOException {
		return new InputStreamReader(
				new URL("http://transport.opendata.ch/v1/connections?from="+
		URLEncoder.encode(from.fullName, "UTF-8") +
		"&to="+ URLEncoder.encode(to.fullName, "UTF-8")).openConnection().getInputStream());
	}

	private List<Reader> loadData() {
		try {
			List<Reader> result = new ArrayList<>();
			for (Journey j : journeys) {
				result.add(connection(j.from, j.to));
			}
			return result;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void renderRadial(Drawable g, BigClockWidget.ClockCoordinates coords, ElementCollection elements) {
		for (Connection c : loadConnections()) {
			if (coords.areMinutesVisible(c.time)) {
				elements.add(new TextElement(
						Format.TIME_PATTERN.format(c.time) +" "+ findShortName(c.from),
						TextElement.Placement.EMPTY_MARKER,
						new Font(Font.SANS_SERIF, Font.PLAIN, 10),
						Color.BLACK,
						TextElement.Parameters.radius(
								coords.convertRadius(0.8),
								coords.convertRadius(0.95))
						.direction(coords.convertMinutes(c.time))
						.width(10, 200)
						.anchor(0, 4)
						));
			}
		}
	}
	
	private String findShortName(String longName) {
		Place bestMatch = null;
		for (Journey j : journeys) {
			if (longName.contains(j.from.fullName) &&
					(bestMatch == null || j.from.fullName.length() > bestMatch.fullName.length())) {
				bestMatch = j.from;
			}
			if (longName.contains(j.to.fullName) &&
					(bestMatch == null || j.to.fullName.length() > bestMatch.fullName.length())) {
				bestMatch = j.to;
			}
		}
		return (bestMatch == null) ? longName : bestMatch.shortName;
	}
}
