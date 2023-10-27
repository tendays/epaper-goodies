/**
 * 
 */
package org.gamboni.pi.epaper.render.weather;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.gamboni.pi.epaper.render.CacheFile;

import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author tendays
 *
 */
public class DarkSkyWeatherProvider implements WeatherProvider {
	private static final Charset CACHE_CHARSET = Charsets.UTF_8;
	
	private final CacheFile<String> cache = new CacheFile<String>(this, Duration.ofHours(1)) {

		@Override
		protected String readFromCache(InputStream input) throws Exception {
			return CharStreams.toString(new InputStreamReader(input, CACHE_CHARSET));
		}

		@Override
		protected String readFromDatasource() throws IOException {
			return CharStreams.toString(loadRawData());
		}

		@Override
		protected void writeToCache(String data, OutputStream out) throws Exception {
			try (Writer w = new OutputStreamWriter(out, CACHE_CHARSET)) {
				w.write(data);
			}
		}
	};

	@Override
	public Data loadData() throws IOException {
		Data data = new Data();
		JsonObject root = new JsonParser().parse(cache.load()).getAsJsonObject();
		data.summary = root.get("hourly").getAsJsonObject().get("summary").getAsString();
		for (JsonObject hour : iterate(root, "hourly")) {
			data.hourly.put(extractTime(hour), extractWeatherPoint(hour));
		}
		
		for (JsonObject day : iterate(root, "daily")) {
			data.daily.put(extractTime(day).toLocalDate(), extractAverageWeatherPoint(day));
		}
		return data;
	}

	private InputStreamReader loadRawData() throws IOException {
		// read key from current directory
		// charset doesn't really matter as long as it's compatible with ASCII
		final String SECRET_KEY = Files.asCharSource(new File("darksky-key"), StandardCharsets.UTF_8).read();
		
		return new InputStreamReader(
				new URL("https://api.darksky.net/forecast/" + SECRET_KEY + "/46.4640011,6.8593927?lang=fr&units=ca")
						.openConnection().getInputStream());
	}
	
	private Iterable<JsonObject> iterate(JsonObject root, String attr) {
		return Iterables.transform(
				root.get(attr).getAsJsonObject().get("data").getAsJsonArray(),
				e -> e.getAsJsonObject());
	}	private ZonedDateTime extractTime(JsonObject element) {
		return ZonedDateTime.ofInstant(Instant.ofEpochMilli(element.get("time").getAsLong() * 1000),
				ZoneId.systemDefault());
	}

	private WeatherPoint extractWeatherPoint(JsonObject json) {
		return new WeatherPoint(json.get("temperature").getAsBigDecimal().setScale(1, RoundingMode.HALF_EVEN),
				json.get("precipIntensity").getAsBigDecimal(),
				json.get("precipProbability").getAsBigDecimal(),
				json.get("icon").getAsString());
	}
	
	private WeatherPoint extractAverageWeatherPoint(JsonObject json) {
		return new WeatherPoint(json.get("temperatureLow").getAsBigDecimal().add(
				json.get("temperatureHigh").getAsBigDecimal()).divide(BigDecimal.valueOf(2), 1, RoundingMode.HALF_EVEN),
				json.get("precipIntensity").getAsBigDecimal(),
				json.get("precipProbability").getAsBigDecimal(),
				json.get("icon").getAsString());
	}
}
