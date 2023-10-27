/**
 * 
 */
package org.gamboni.pi.epaper.render.server;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gamboni.pi.epaper.render.Layout;
import org.gamboni.pi.epaper.render.calendar.EventInfo;

import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.Gson;

import org.gamboni.pi.watchy.WatchyData;
import spark.Spark;

/**
 * @author tendays
 *
 */
public class EpaperServer {
	public static class EventDto {
		public String text;
		public long from;
		public long to;
	}

	private final Layout config;

	public EpaperServer(String configFile) throws IOException {
		this.config = Layout.parseConfigFile(configFile, false);
	}
	@SuppressWarnings("serial")
	public void run() {
		Spark.exception(Exception.class, (t, req, res) -> {
			t.printStackTrace();
			res.status(500);
		});
		
		Spark.port(8080);
		Spark.get("/", (req, res) -> {
			return "<html><body><form method='post' action='/off'><input type='submit' value='éteindre'></input></form></body></html>";
		});
		Spark.get("/calendar", (req, res) -> {
			LocalDate today = LocalDate.now();
			LocalDate tomorrow = today.plusDays(1);
			List<EventDto> events = loadEvents(today, tomorrow)
					.map(ev -> {
						EventDto dto = new EventDto();
						dto.text = ev.text;
						dto.from = ev.from.toEpochSecond();
						dto.to = ev.to.toEpochSecond();
						return dto;
					})
					.collect(Collectors.toList());

			return new Gson().toJson(events,
					new TypeToken<Set<EventDto>>() {}.getType());
		});
		Spark.get("/watchy", (req, res) -> {
			WatchyData result = new WatchyData();

			LocalDate today = LocalDate.now();
			LocalDate tomorrow = today.plusDays(2);

			result.calendar = loadEvents(today, tomorrow)
					.map(WatchyData.WatchyEvent::new)
					.collect(Collectors.toList());
			result.weather = config.getWeather().loadData().hourly
					.entrySet()
					.stream()
					.map(entry ->
							new WatchyData.WeatherWithTime(entry.getKey(), entry.getValue()))
					.collect(Collectors.toList());
			return new Gson().toJson(result);
		});
		Spark.post("/off", (req, res) -> {
			System.err.println("Received shutdown request...");
			new Thread(() -> {
				System.err.println("Shutting down in two seconds...");
				Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
				System.err.println("Shutting down NOW!");
				try {
					Runtime.getRuntime().exec("sudo halt");
				} catch (IOException e) {
					System.err.println("Failed shutting down?");
					e.printStackTrace();
				}
			}).start();
			return "Attends que la lampe verte s'éteigne avant de tirer la prise...";
		});
	}

	private Stream<EventInfo> loadEvents(LocalDate from, LocalDate to) {
		return config.getCalendars().stream()
				.flatMap(calendar -> {
					try {
						return calendar.loadData(from, to).stream();
					} catch (Throwable t) {
						t.printStackTrace();
						return Stream.of();
					}
				})
				.sorted();
	}
}

