/**
 * 
 */
package org.gamboni.pi.epaper.render.weather;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author tendays
 *
 */
public interface WeatherProvider {

	WeatherProvider NULL = new WeatherProvider() {
		@Override
		public Data loadData() throws IOException {
			return new Data();
		}
	};

	public static class Data implements Serializable {
		public final Map<ZonedDateTime, WeatherPoint> hourly = new TreeMap<>();
		public final Map<LocalDate, WeatherPoint> daily = new TreeMap<>();
		public String summary = "";

		public String getHourlyIcon(ZonedDateTime time) {
			return this.hourly.get(time).icon;
		}

		public String getDailyIcon(LocalDate time) {
			WeatherPoint weatherPoint = this.daily.get(time);
			return (weatherPoint == null) ? null : weatherPoint.icon;
		}
	}

	public static class WeatherPoint implements Serializable {
		public final BigDecimal temperature;
		public final BigDecimal precipitation;
		public final BigDecimal precipitationProbability;
		public final String icon;

		public WeatherPoint(BigDecimal temperature, BigDecimal precipitation, BigDecimal precipitationProbability,
				String icon) {
			this.temperature = temperature;
			this.precipitation = precipitation;
			this.precipitationProbability = precipitationProbability;
			this.icon = icon;
		}

		public WeatherPoint(WeatherPoint that) {
			this.temperature = that.temperature;
			this.precipitation = that.precipitation;
			this.precipitationProbability = that.precipitationProbability;
			this.icon = that.icon;
		}
	}

	Data loadData() throws IOException;

}
