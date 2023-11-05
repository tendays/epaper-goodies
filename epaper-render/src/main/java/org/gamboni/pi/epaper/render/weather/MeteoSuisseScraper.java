/**
 * 
 */
package org.gamboni.pi.epaper.render.weather;

import java.io.*;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.gamboni.pi.epaper.render.CacheFile;

/**
 * @author tendays
 *
 */
public class MeteoSuisseScraper extends CacheFile<MeteoSuisseScraper.Data> {
	private final int zip;

	public static class WeatherDiagramItem implements Serializable {
		List<List<BigDecimal>> rainfall;
		List<List<BigDecimal>> sunshine;
		Symbol symbol_day;
		List<Symbol> symbols;
		List<List<BigDecimal>> temperature;
		List<List<BigDecimal>> varianceRain;
		List<List<BigDecimal>> varianceRange;
		WindData windGustPeak;
		WindData wind;
	}

	/**
	 *
	 * {"currentVersionDirectory":"version__20221114_0935"}
	 */
	static class ChartVersions {
		String currentVersionDirectory;
	}

	public static class WindData implements Serializable {
		List<List<BigDecimal>> data;
		List<WindDirection> symbols;
	}

	public static class WindDirection implements Serializable {
		String symbolId;
		long timestamp;
	}

	public static class Symbol implements Serializable {
		int weather_symbol_id;
		long timestamp;

		public String getIcon() {
			return "meteosuisse-icons/" + weather_symbol_id;
		}
	}

	public static class Data implements Serializable {
		int zip;
		public List<WeatherDiagramItem> weatherDiagram;
		public String weatherReport;
	}

	public MeteoSuisseScraper(int zip) {
		super(MeteoSuisseScraper.class, zip + "", Duration.ofHours(1));
		this.zip = zip;
	}

	private static URL newUrl(String url) {
		try {
			return new URL(url);
		} catch (MalformedURLException ohReally) {
			throw new RuntimeException(ohReally);
		}
	}

	@Override
	protected Data readFromCache(InputStream input) throws Exception {
		Data fromCache = (Data)new ObjectInputStream(input).readObject();
		if (fromCache.zip != this.zip) {
			throw new IllegalStateException("Cached: "+ fromCache.zip +". Requested: "+ this.zip);
		}
		return fromCache;
	}

	@Override
	protected Data readFromDatasource() throws IOException {
		Data data = new Data();
		data.zip = this.zip;

		String directory = load("forecast-chart/versions.json", ChartVersions.class).currentVersionDirectory;
		data.weatherDiagram = load("forecast-chart/" +
				directory + "/fr/" + zip + ".json", new TypeToken<List<WeatherDiagramItem>>(){});

		String weatherReportVersion = load("versions.json", new TypeToken<Map<String, String>>() {
		}).get("weather-report/fr/west");

		if (weatherReportVersion != null) {
			StringWriter reportOut = new StringWriter();
			readUrl("weather-report/fr/west/version__" + weatherReportVersion + "/textproduct_fr.xhtml").transferTo(reportOut);
			data.weatherReport = reportOut.toString();
		}
		return data;
	}

	private static <T> T load(String path, Class<T> type) throws IOException {
		return new Gson().fromJson(readUrl(path), type);
	}

	private static <T> T load(String path, TypeToken<T> token) throws IOException {
		return new Gson().fromJson(readUrl(path), token.getType());
	}

	private static InputStreamReader readUrl(String spec) throws IOException {
		return new InputStreamReader(new URL("https://www.meteosuisse.admin.ch/product/output/" + spec).openConnection()
				.getInputStream(), StandardCharsets.UTF_8);
	}

}
