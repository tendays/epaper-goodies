/**
 * 
 */
package org.gamboni.pi.epaper.render.weather;

import com.google.common.base.Splitter;
import org.gamboni.pi.epaper.render.widget.motd.MotdWidget;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

/**
 * @author tendays
 *
 */
public class MeteoSuisseWeatherProvider implements WeatherProvider, MotdWidget.Face {

	public static final BigDecimal TWO = BigDecimal.valueOf(2);
	final int zip;

	/** For testing: pass the zip code as parameter. */
	public static void main(String[] a) {
		try {

			MeteoSuisseWeatherProvider instance = new MeteoSuisseWeatherProvider(Integer.parseInt(a[0]));

			instance.loadData().hourly.forEach((time, weather) -> {
				System.out.println(time.toString() +" "+ weather.temperature +"°C, "+ weather.precipitation +"mm");
			});

			instance.getMotd().ifPresent(m ->
			System.out.println(m.text));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static ZonedDateTime getTime(BigDecimal millis) {
		return getTime(millis.longValue());
	}
	private static ZonedDateTime getTime(long millis) {
		return ZonedDateTime.ofInstant(
				Instant.ofEpochMilli(millis), ZoneId.systemDefault());
	}

	public MeteoSuisseWeatherProvider(int zip) {
		this.zip = zip;
	}

	public Data loadData() throws IOException {
		MeteoSuisseScraper.Data source = new MeteoSuisseScraper(this.zip).load();
		Data result = new Data();

		Map<ZonedDateTime, BigDecimal> temperature = new HashMap<>();
		Map<ZonedDateTime, BigDecimal> rain = new HashMap<>();
		Map<ZonedDateTime, String> symbols = new HashMap<>();

		source.weatherDiagram.forEach(item -> {
					for (List<BigDecimal> tempRecord : item.temperature) {
						temperature.put(getTime(tempRecord.get(0)),
								tempRecord.get(1));
					}
					for (List<BigDecimal> rainRecord : item.rainfall) {
						rain.put(getTime(rainRecord.get(0)),
								rainRecord.get(1).multiply(TWO));
					}
					for (MeteoSuisseScraper.Symbol symbol : item.symbols) {
						// we seem to be getting weather symbols at half hours
						symbols.put(getTime(symbol.timestamp).withMinute(0),
								symbol.getIcon());
					}
				});

		Map<LocalDate, Double> dayTemperatures = temperature.entrySet().stream()
						.collect(Collectors.groupingBy(
								entry -> entry.getKey().toLocalDate(),
										Collectors.averagingDouble(entry -> entry.getValue()
												.doubleValue()))
								);

		source.weatherDiagram.forEach(item -> {
			LocalDate day = getTime(item.symbol_day.timestamp).toLocalDate();
			result.daily.put(day, new WeatherPoint(
				BigDecimal.valueOf(dayTemperatures.get(day)).setScale(1, RoundingMode.HALF_EVEN), null, null, item.symbol_day.getIcon()));
		});

		result.hourly.putAll(temperature.entrySet().stream().collect(toMap(
				Map.Entry::getKey,
				tempEntry -> new WeatherPoint(
						tempEntry.getValue(),
						rain.get(tempEntry.getKey()),
						BigDecimal.ONE,
						symbols.get(tempEntry.getKey())))));

		return result;
	}

	@Override
	public Optional<MotdWidget.Message> getMotd() {
		try {
			MeteoSuisseScraper.Data source = new MeteoSuisseScraper(this.zip).load();

			if (source.weatherReport != null) {
				var builder = new StringBuilder();
				for (var heading : Jsoup.parse(source.weatherReport).select("h4")) {
					if (heading.wholeOwnText().startsWith("Aujourd'hui")) {
						for (var paragraph = heading.nextElementSibling();
							 paragraph != null && paragraph.tagName().equalsIgnoreCase("p");
							 paragraph = paragraph.nextElementSibling()) {
							if (paragraph.wholeOwnText().trim().startsWith("En montagne")) { continue; }

							for (var sentence: Splitter.on(Pattern.compile("\\. *")).trimResults().omitEmptyStrings().split(paragraph.wholeOwnText())) {
								if (sentence.startsWith("En Valais") || sentence.startsWith("En montagne")) {
									continue;
								}
								int comma = sentence.lastIndexOf(',');
								if (comma != -1 && sentence.lastIndexOf("Valais") > comma) {
									sentence = sentence.substring(0, comma);
								}
								builder.append(sentence +". ");
							}
						}
					}
				}
				return Optional.of(new MotdWidget.Message(builder.toString(), MotdWidget.Priority.BACKGROUND));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Optional.empty();
	}
}
