/**
 * 
 */
package org.gamboni.pi.epaper.render.widget;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.gamboni.pi.epaper.render.Layout;
import org.gamboni.pi.epaper.render.gfx.Drawable;
import org.gamboni.pi.epaper.render.gfx.Interpolation;
import org.gamboni.pi.epaper.render.gfx.layout.*;
import org.gamboni.pi.epaper.render.gfx.layout.Coords;
import org.gamboni.pi.epaper.render.gfx.layout.ElementCollection;
import org.gamboni.pi.epaper.render.gfx.layout.IconElement;
import org.gamboni.pi.epaper.render.gfx.layout.PolarCoords;
import org.gamboni.pi.epaper.render.gfx.layout.RectCoords;
import org.gamboni.pi.epaper.render.gfx.layout.TextElement;
import org.gamboni.pi.epaper.render.weather.WeatherProvider;
import org.gamboni.pi.epaper.render.weather.WeatherProvider.Data;
import org.gamboni.pi.epaper.render.weather.WeatherProvider.WeatherPoint;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;

/**
 * @author tendays
 *
 */
public class WeatherWidget implements Widget, TemporalFace, CrownFace {
	
	private final Layout config;
	
	public WeatherWidget(Layout config) {
		this.config = config;
	}
	
	private static final int LABEL_SIZE = 8;
	
	private static class DataPoint {
		final ZonedDateTime time;
		final BigDecimal value;
		
		public DataPoint(ZonedDateTime time, BigDecimal value) {
			this.time = time;
			this.value = value;
		}
		
		public String toString() {
			return time +" "+ value;
		}
	}
	
	private static class RainPoint {
		final ZonedDateTime time;
		final BigDecimal value;
		final BigDecimal probability;
		public RainPoint(ZonedDateTime time, BigDecimal value, BigDecimal probability) {
			this.time = time;
			this.value = value;
			this.probability = probability;
		}
		public String toString() {
			return time +" "+ value +"("+ probability.multiply(BigDecimal.valueOf(100)) +"%)";
		}
	}
	
	private static class Plot implements Iterable<DataPoint> {
		final ImmutableList<DataPoint> points;
		public Plot(Iterable<DataPoint> points) {
			this.points = ImmutableList.copyOf(points);
		}
		
		private <T extends Comparable<? super T>> Range<T> getRange(Function<DataPoint, T> attribute) {
			T min = null;
			T max = null;
			for (DataPoint p : points) {
				T t = attribute.apply(p);
				if (min == null) {
					min = t;
					max = t;
				} else if (t.compareTo(min) < 0) {
					min = t;
				} else if (t.compareTo(max) > 0) {
					max = t;
				}
			}
			return Range.closed(min, max);
		}
		
		Range<ZonedDateTime> getTimeRange() {
			return this.getRange(p -> p.time);
		}
		
		Range<BigDecimal> getValueRange() {
			return this.getRange(p -> p.value);
		}

		@Override
		public Iterator<DataPoint> iterator() {
			return points.iterator();
		}

		public int count() {
			return points.size();
		}
		
		public DataPoint get(int i) {
			return points.get(i);
		}
	}
	
	private static class CoordinateTransform implements Function<DataPoint, Coords> {
		final Range<ZonedDateTime> xRange;
		final Range<BigDecimal> yRange;
		final int left, top;
		final int width, height;
		
		public CoordinateTransform(Range<ZonedDateTime> xRange, Range<BigDecimal> yRange,
				int left, int top, int width, int height) {
			this.xRange = xRange;
			this.yRange = yRange;
			this.left = left;
			this.top = top;
			this.width = width;
			this.height = height;
		}

		@Override
		public Coords apply(DataPoint t) {
			return new RectCoords(
					/* Distance to left (min) */
					left + Duration.between(xRange.lowerEndpoint(), t.time).getSeconds()
					* width
					/ Duration.between(xRange.lowerEndpoint(), xRange.upperEndpoint()).getSeconds(),
					/* Distance to top (max) */
					top + yRange.upperEndpoint().subtract(t.value).divide(
							yRange.upperEndpoint().subtract(yRange.lowerEndpoint()), 8, RoundingMode.HALF_EVEN)
					.doubleValue() * height);
		}
	}
	
	private static final int iconHeight = 25;
	private WeatherProvider.Data data;
	public void render(Drawable graphics, int w, int h) {
		Plot temperaturePlot = temperaturePlot(getData().hourly.entrySet());
		
		int plotLeft = 15;
		int fontHeight = 6;
		CoordinateTransform coordTransform = new CoordinateTransform(
				temperaturePlot.getTimeRange(),
				temperaturePlot.getValueRange(),
				plotLeft, iconHeight,
				w - plotLeft - 1,
				h - fontHeight - iconHeight - 1);
		
		
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, w, h);
		
		graphics.setColor(Color.BLACK);
		graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));

		/* Vertical axis (temperature) */
		BigDecimal minTemp = temperaturePlot.getValueRange().lowerEndpoint();
		BigDecimal maxTemp = temperaturePlot.getValueRange().upperEndpoint();
		
		BigDecimal deltaTemp = maxTemp.subtract(minTemp);
		BigDecimal vStep = selectStep(deltaTemp, BigDecimal.TEN, 1, 2, 5, 10);
		
		/* Round up to the next step multiple */
		for (BigDecimal value = minTemp.divide(vStep, 0, RoundingMode.UP).multiply(vStep).setScale(vStep.scale());
				value.compareTo(maxTemp) < 0;
				value = value.add(vStep)) {
			
			int y = maxTemp.subtract(value)
					.multiply(BigDecimal.valueOf(coordTransform.height))
					.divide(deltaTemp, RoundingMode.HALF_EVEN)
					.intValue() + coordTransform.top;
			
			graphics.drawString(value.toPlainString(), 0, y + fontHeight / 2);
			
			dottedLine(graphics, coordTransform.left, y, 4, 0, coordTransform.left + coordTransform.width, y);
		}
		
		/* Horizontal axis */
		ZonedDateTime minTime = temperaturePlot.getTimeRange().lowerEndpoint();
		ZonedDateTime maxTime = temperaturePlot.getTimeRange().upperEndpoint();
		
		BigDecimal minHours = BigDecimal.valueOf(minTime.toEpochSecond()).divide(BigDecimal.valueOf(3600),
				2, RoundingMode.HALF_UP);
		BigDecimal maxHours = BigDecimal.valueOf(maxTime.toEpochSecond()).divide(BigDecimal.valueOf(3600),
				2, RoundingMode.HALF_UP);
		
		// don't go below one hour
		BigDecimal hStep = BigDecimal.ONE.max(this.selectStep(maxHours.subtract(minHours), BigDecimal.valueOf(30), 1, 2, 3, 4, 6, 12));

		/* Round up to the next step multiple */
		for (ZonedDateTime value = minTime.withHour(BigDecimal.valueOf(minTime.getHour()).divide(hStep, 0, RoundingMode.UP).multiply(hStep).setScale(0).intValueExact());
				value.compareTo(maxTime) < 0;
				value = value.plusHours(hStep.intValueExact())) {
			
			int x = BigDecimal.valueOf(Duration.between(minTime, value).getSeconds())
					.multiply(BigDecimal.valueOf(coordTransform.width))
					.divide(BigDecimal.valueOf(Duration.between(minTime, maxTime).getSeconds()), RoundingMode.HALF_EVEN)
					.intValue() + coordTransform.left;
			
			graphics.drawString(String.valueOf(value.getHour()),
					x,
					coordTransform.top + coordTransform.height + fontHeight);
			dottedLine(graphics, x, coordTransform.top, 0, 2, x, coordTransform.top + coordTransform.height);
		}
		
		/* Temperature plot */
		Coords pen = null;
		int iconX = 0;
		int iconY = h;
		for (int i = 0; i < temperaturePlot.count(); i++) {
			DataPoint point = temperaturePlot.get(i);
			Coords to = coordTransform.apply(point);
			iconY = Math.min(iconY, (int)to.getY());
			if (pen != null) {
				graphics.drawLine(pen, to);
				if (i % 3 == 0) {
					String icon = data.getHourlyIcon(point.time);
					graphics.drawIcon(icon, new RectCoords(iconX, iconY), 50, Drawable.Anchor.CENTRE, Drawable.ImageStyle.WHITE_TRANSPARENT);
					iconY = h;
				}
			}

			if (i % 3 == 0) {
				iconX = (int)to.getX();
			}
			pen = to;
		}
	}

	private Plot temperaturePlot(Iterable<Entry<ZonedDateTime, WeatherPoint>> hourlyEntries) {
		return new Plot(Iterables.transform(hourlyEntries,
				e -> new DataPoint(e.getKey(), e.getValue().temperature)));
	}

	private Iterable<RainPoint> precipitationPlot(Iterable<Entry<ZonedDateTime, WeatherPoint>> hourlyEntries) {
		return Iterables.transform(hourlyEntries,
				e -> new RainPoint(
						e.getKey(),
						e.getValue().precipitation,
						e.getValue().precipitationProbability));
	}

	private Data getData() {
		if (this.data == null) {
			try {
				this.data = config.getWeather().loadData();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return data;
	}

	private BigDecimal selectStep(BigDecimal deltaTemp, BigDecimal maxTicks, int... presets) {
		BigDecimal minStep = deltaTemp.divide(maxTicks, deltaTemp.scale() + 1, RoundingMode.UP).stripTrailingZeros();
		BigDecimal singleDigitStep = minStep.setScale(minStep.scale() - minStep.precision() + 1, RoundingMode.UP);
		int digit = singleDigitStep.scaleByPowerOfTen(singleDigitStep.scale()).intValueExact();
		
		int digitToUse = presets[0]; // used in case there's only one preset, or digit is smaller than the first preset
		
		for (int i = presets.length - 2; i >= 0; i--) {
			if (digit > presets[i]) {
				digitToUse = presets[i+1];
				break;
			}
		}
		
		BigDecimal step = BigDecimal.valueOf(digitToUse).scaleByPowerOfTen( - singleDigitStep.scale());
		return step;
	}
	
	private void dottedLine(Drawable g, int x1, int y1, int dx, int dy, int x2, int y2) {
		int x = x1;
		int y = y1;
		// loop as long as dx and dy have the correct sign
		while ((x2 - x) * dx >= 0 && (y2 - y) * dy >= 0) {
			g.drawPixel(x, y);
			x += dx;
			y += dy;
		}
	}

	@Override
	public void renderRadial(Drawable g, BigClockWidget.ClockCoordinates clock, ElementCollection elements) {
		List<Map.Entry<ZonedDateTime, WeatherPoint>> weatherPoints = ImmutableList.copyOf(getData().hourly.entrySet());
		
		int firstVisibleItem = Iterables.indexOf(weatherPoints, e -> clock.areHoursVisible(e.getKey()));
		if (firstVisibleItem == -1) { return; }
		
		int lastVisibleItem = weatherPoints.size() - Iterables.indexOf(Lists.reverse(weatherPoints), e -> clock.areHoursVisible(e.getKey())) - 1;
		
		/* Expand range by one to allow interpolating between first/last visible item and its hidden neighbour */
		int firstRelevantItem = Math.max(0, firstVisibleItem - 1);
		int lastRelevantItem = Math.min(weatherPoints.size() - 1, lastVisibleItem + 1);
		
		List<Map.Entry<ZonedDateTime, WeatherPoint>> visibleData = weatherPoints.subList(firstVisibleItem, lastVisibleItem + 1);
		List<Map.Entry<ZonedDateTime, WeatherPoint>> relevantData = weatherPoints.subList(firstRelevantItem, lastRelevantItem + 1);

		/* Rain curve */
		//g.setColor(Color.BLACK);
		radialBarPlot(g, clock, precipitationPlot(relevantData));
		
		/* Temperature curve */
		
		Plot relevantTplot = temperaturePlot(relevantData);
		Plot visibleTplot = temperaturePlot(visibleData);
		
		BigDecimal minTemp = visibleTplot.getValueRange().lowerEndpoint();
		BigDecimal maxTemp = visibleTplot.getValueRange().upperEndpoint();
		BigDecimal deltaTemp = maxTemp.subtract(minTemp);

		/* Concentric circles for some round values */
		BigDecimal step = selectStep(deltaTemp, /*maxTicks*/BigDecimal.valueOf(4), 1, 2, 5, 10);
		/* Round up to the next step multiple */
		for (BigDecimal value = minTemp.divide(step, 0, RoundingMode.UP).multiply(step).setScale(step.scale());
				value.compareTo(maxTemp) < 0;
				value = value.add(step)) {
			g.setColor(Color.PINK);
			g.setStroke(3);
			int radius = (int)clock.convertRadius(
					temperatureRadiusRatio(visibleTplot, value));
			g.drawCircle(0, 0, radius);
			
			g.setStroke(1);
			g.setColor(Color.RED);
			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, LABEL_SIZE));
			g.drawString(value +"°C", 0, - radius);
		}
		
		/* Highest and lowest temperature labels */
		for (DataPoint point : visibleTplot.points) {
			PolarCoords next = temperatureToCoord(clock, visibleTplot, point.time, point.value);

			if (point.value.equals(visibleTplot.getValueRange().upperEndpoint()) ||
					point.value.equals(visibleTplot.getValueRange().lowerEndpoint())) {

				elements.add(new TextElement(point.value.toString() +"°C",
						TextElement.Placement.FILLED_MARKER,
						new Font(Font.SANS_SERIF, Font.PLAIN, 10),
						Color.RED,
						TextElement.Parameters.radius(next.radius)
						.direction(next.angle)
						.width(1, 100) // allow splitting between N and °C if space is short (currently doesn't work because can't split around '°')
						.anchor(0, 4)
						));
			}
			
			/*if (pen != null) {
				g.drawLine(pen, next);
			}
			pen = next;*/
		}
		
		radialSplinePlot(g, clock, relevantTplot, visibleTplot);		
		
		/* icons */
		for (DataPoint point : visibleTplot.points) {
			if (clock.areHoursVisible(point.time)) {
				String icon = data.getHourlyIcon(point.time);
				if (icon != null) {
					elements.add(new IconElement(icon, AbstractPolarElement.Parameters.radius(clock.convertRadius(0.8))
							.direction(clock.convertHours(point.time)), 50, Drawable.ImageStyle.WHITE_TRANSPARENT));
				}
			}
		}
	}

	private void radialSplinePlot(Drawable g, BigClockWidget.ClockCoordinates clock, Plot relevantTplot, Plot visibleTplot) {
		Interpolation interpolation = new Interpolation(Iterables.transform(relevantTplot.points, dp -> dp.value.doubleValue()));
		ZonedDateTime now = ZonedDateTime.now();
		ZonedDateTime end = now.plusHours(12);
		long epochReference = Iterables.getFirst(
				Iterables.transform(relevantTplot.points, dp -> dp.time), now).toEpochSecond();

		PolarCoords pen = null;
		for (ZonedDateTime t = now; !t.isAfter(end); t = t.plusMinutes(10)) {
			PolarCoords next = temperatureToCoord(clock, visibleTplot, t,
					BigDecimal.valueOf(interpolation.evaluate((t.toEpochSecond() - epochReference) / 3600.0)));
			
			if (pen != null) {
				g.drawLine(pen, next);
			}
			pen = next;
		}
	}

	private void radialBarPlot(Drawable g, BigClockWidget.ClockCoordinates clock, Iterable<RainPoint> precipitations) {
		ZonedDateTime now = ZonedDateTime.now();
		ZonedDateTime end = now.plusHours(12);
		List<Coords> uncertainPolygon = new ArrayList<>();
		List<Coords> darkPolygon = new ArrayList<>();
	
		ZonedDateTime pointer = now;
		
		BigDecimal uncertain = BigDecimal.ZERO;
		BigDecimal amplitude = BigDecimal.ZERO;
		for (RainPoint point : precipitations) {
			/*Point2D next = precipitationToCoord(clock, point.time, point.value);
			g.drawLine(clock.center(), next);
			g.drawCircle(next, 3);*/

			
			amplitude = point.value.multiply(point.probability);
			uncertain = point.value;
			darkPolygon.add(precipitationToCoord(clock, pointer, amplitude));
			uncertainPolygon.add(precipitationToCoord(clock,  pointer, uncertain));
			
			pointer = min(end, point.time.plusMinutes(30));
			
			darkPolygon.add(precipitationToCoord(clock, pointer, amplitude));
			uncertainPolygon.add(precipitationToCoord(clock,  pointer, uncertain));
		}
		
		// in case the last sector did not reach the end, continue it, with the same amplitude
		if (!pointer.equals(end)) {
			darkPolygon.add(precipitationToCoord(clock, end, amplitude));
			uncertainPolygon.add(precipitationToCoord(clock, end, uncertain));
		}
		g.setColor(Color.GRAY);
		g.fillPolygon(uncertainPolygon);
		g.setColor(Color.BLACK);
		g.fillPolygon(darkPolygon);
	}
	
	static <T extends Comparable<? super T>> T max(T a, T b) {
		if (a.compareTo(b) > 0) {
			return a;
		} else {
			return b;
		}
	}
	
	static <T extends Comparable<? super T>> T min(T a, T b) {
		if (a.compareTo(b) > 0) {
			return b;
		} else {
			return a;
		}
	}
	
	private double temperatureRadiusRatio(Plot plot, BigDecimal value) {
		BigDecimal valueRange = plot.getValueRange().upperEndpoint().subtract(plot.getValueRange().lowerEndpoint());
		return value.subtract(plot.getValueRange().lowerEndpoint())
		.divide(valueRange, 5, RoundingMode.HALF_EVEN)
		.doubleValue() * 0.5 + 0.2;
	}

	private PolarCoords temperatureToCoord(BigClockWidget.ClockCoordinates clock, Plot plot, ZonedDateTime time, BigDecimal value) {
		return new PolarCoords(clock.convertRadius(temperatureRadiusRatio(plot, value)), clock.convertHours(time));
	}

	private PolarCoords precipitationToCoord(BigClockWidget.ClockCoordinates clock, ZonedDateTime time, BigDecimal value) {
		return new PolarCoords(clock.convertRadius(value.doubleValue() / 6), clock.convertHours(time));
	}

	@Override
	public void renderOnCalendar(Drawable g, int left, int top, int width, int height, LocalDate date) {
		String icon = getData().getDailyIcon(date);
		if (icon != null) {
			g.drawIcon(icon, new RectCoords(left, top), 50, Drawable.Anchor.CENTRE, Drawable.ImageStyle.WHITE_TRANSPARENT);
			// correct coordinates: drawIcon(g, icon, left + width - icon.getWidth()/2, top + height - icon.getHeight()/3);
		}
		WeatherPoint dayWeather = getData().daily.get(date);
		if (dayWeather != null) {
			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
			g.drawString(dayWeather.temperature +"°C", left + width - 40, top + 16);
		}
	}

	@Override
	public void renderCrown(Drawable g, BigClockWidget.ClockCoordinates coords, LocalDate date, ElementCollection elements) {
		String icon = getData().getDailyIcon(date);
		
		ZonedDateTime day = date.atStartOfDay(ZoneId.systemDefault());
		AbstractPolarElement.Wa polarBounds = AbstractPolarElement.Parameters.radius(coords.convertRadius(0.1), coords.convertRadius(1))
		.direction(coords.convertDays(day.withHour(4)),
				coords.convertDays(day.withHour(20)));
		
		if (icon != null) {
			elements.add(new IconElement(icon, polarBounds, 40, Drawable.ImageStyle.WHITE_TRANSPARENT));
		}
		WeatherPoint dayWeather = getData().daily.get(date);
		if (dayWeather != null && dayWeather.temperature != null) {
			elements.add(new TextElement(
					dayWeather.temperature +"°C",
					TextElement.Placement.CENTRED,
					new Font(Font.SANS_SERIF, Font.PLAIN, 9),
					Color.RED,
					polarBounds.width(10, 50).anchor(0, 4)
					));
		}
	}
	
	
}
