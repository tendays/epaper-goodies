/**
 * 
 */
package org.gamboni.pi.epaper.render.weather;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.gamboni.pi.epaper.render.CacheFile;
import org.gamboni.pi.epaper.render.widget.motd.MotdWidget;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * @author tendays
 *
 */
public class WeatherDangerMotd implements MotdWidget.Face {
	private static final Set<Integer> REGIONS = ImmutableSet.of(227, 434);
	private final CacheFile<MeteoSuisseScraper.Data> cache = new MeteoSuisseScraper(1800);
	
	public static void main(String[] test) {
		new WeatherDangerMotd().getMotd().ifPresent(System.out::println);
	}

	private static final Map<String, String> WARN_TYPES = ImmutableMap.<String, String>builder()
			.put("frost", "Gel")
			.put("heat-wave", "Canicule")
			.put("rain", "Pluie")
			.put("slippery-roads", "Verglas")
			.put("snow", "Neige")
			.put("thunderstorm", "Orage")
			.put("wind", "Vent")
			.build();

	@Override
	public Optional<MotdWidget.Message> getMotd() {
		try {
			throw new UnsupportedOperationException();
			/*
			Entry<String, JsonElement> nextDayDanger = new JsonParser().parse(cache.load())
					.getAsJsonObject()
					.get("days")
					.getAsJsonObject()
					.entrySet()
					.iterator()
					.next();

			String result = nextDayDanger
					.getValue()
					.getAsJsonObject()
					.get("hazards")
					.getAsJsonObject()
					.entrySet()
					.stream()
					.map(Entry::getValue)
					.filter(JsonElement::isJsonArray)
					.flatMap(j -> Streams.stream(j.getAsJsonArray()))
					.map(JsonElement::getAsJsonObject)
					.filter(j -> Iterables.any(j.get("areas").getAsJsonArray(),
							area -> REGIONS.contains(area.getAsInt())))
					.map(j -> {
						String warnType = j.get("warn_type").getAsString();
						return WARN_TYPES.getOrDefault(warnType, warnType) + ", danger niveau " + j.get("warnlevel")
								+ ":" +
								Html.extractText(j.get("description").getAsString().replace("\n", " "));
					})
					.collect(Collectors.joining("\n"));

			if (result.isEmpty()) {
				return Optional.empty();
			} else {
				return Message.of(result, Priority.IMPORTANT);
			}*/
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Optional.empty();
	}
}
