/**
 * 
 */
package org.gamboni.pi.epaper.render;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Iterators;
import org.gamboni.pi.epaper.render.calendar.CalendarLoader;
import org.gamboni.pi.epaper.render.gfx.Drawable;
import org.gamboni.pi.epaper.render.gfx.layout.IntRectangle;
import org.gamboni.pi.epaper.render.weather.WeatherProvider;
import org.gamboni.pi.epaper.render.widget.ClockFace;
import org.gamboni.pi.epaper.render.widget.Widget;
import org.gamboni.pi.epaper.render.widget.motd.MotdWidget;

import com.google.common.io.CharStreams;

/**
 * @author tendays
 *
 */
public class Layout {
	List<WidgetAtRectangle> widgets = new ArrayList<>();
	List<CalendarLoader> calendars = new ArrayList<>();
	List<MotdWidget.Face> motds = new ArrayList<>();
	List<ClockFace> clocks = new ArrayList<>();
	List<Consumer<ClockFace>> clockConsumers = new ArrayList<>();

	WeatherProvider weather = WeatherProvider.NULL;
	private int width;
	private int height;

    private static class WidgetAtRectangle {
		final Widget widget;
		final IntRectangle rect;

		public WidgetAtRectangle(Widget widget, IntRectangle rect) {
			this.widget = widget;
			this.rect = rect;
		}
	}

	public void add(Widget item, IntRectangle rect) {
		widgets.add(new WidgetAtRectangle(item, rect));
	}

	public void add(ClockFace item) {
		clocks.add(item);
		clockConsumers.forEach(c -> c.accept(item));
	}

	public void add(CalendarLoader calendar) {
		calendars.add(calendar);
	}

	public void add(MotdWidget.Face motd) {
		motds.add(motd);
	}

	public void setWeather(WeatherProvider weather) {
		this.weather = weather;
	}

	public void render(Drawable graphics) {
		for (WidgetAtRectangle w : widgets) {
			graphics.render(w.widget, w.rect.left, w.rect.top, w.rect.right, w.rect.bottom);
		}
	}

	public WeatherProvider getWeather() {
		return weather;
	}

	public List<CalendarLoader> getCalendars() {
		return calendars;
	}

	public void getClockFaces(Consumer<ClockFace> consumer) {
		clocks.forEach(consumer);
		this.clockConsumers.add(consumer);
	}

	public List<MotdWidget.Face> getMotds() {
		return motds;
	}

	public static Layout parseConfigFile(String layoutFile, boolean graphical)
			throws IOException, FileNotFoundException {
		Layout layout = new Layout();
		Pattern widgetLine = Pattern.compile("^([0-9]+) ([0-9]+) ([0-9]+) ([0-9]+) (.*)$");
		try (InputStreamReader reader = new InputStreamReader(new FileInputStream(layoutFile),
				StandardCharsets.UTF_8)) {
			Iterator<String> lines = Iterators.filter(
					CharStreams.readLines(reader).iterator(),
				l -> !l.isBlank() && !l.startsWith("#"));

			if (graphical) {
				// first line: output format
				if (!lines.hasNext()) {
					throwBadFirstLine();
				}
				Matcher outputMatch = Pattern.compile("^([0-9]+)x([0-9]+)$").matcher(lines.next().trim());
				if (!outputMatch.matches()) {
					throwBadFirstLine();
				}
				int width = Integer.parseInt(outputMatch.group(1));
				int height = Integer.parseInt(outputMatch.group(2));
				layout.setDimensions(width, height);
			}
			for (String line : (Iterable<String>) () -> lines) {
				IntRectangle rect;
				if (graphical) {
					Matcher widgetMatch = widgetLine.matcher(line);
					if (widgetMatch.matches()) {
						rect = new IntRectangle(
								Integer.parseInt(widgetMatch.group(1)),
								Integer.parseInt(widgetMatch.group(2)),
								Integer.parseInt(widgetMatch.group(3)),
								Integer.parseInt(widgetMatch.group(4)));
						line = widgetMatch.group(5);
					} else {
						rect = null;
					}
				} else {
					rect = null;
				}
				if (!Widgets.parse(layout, rect, line)) {
					System.err.println("Can't parse this line: " + line);
				}
			}

		}
		return layout;
	}

	private static void throwBadFirstLine() {
		throw new IllegalArgumentException("First layout must specify target size, for instance 1600x1200");
	}

	private void setDimensions(int width, int height) {
		this.width = width;
		this.height = height;
	}
	
	public <T> T getDimensions(BiFunction<Integer, Integer, T> constructor) {
		return constructor.apply(width, height);
	}
}
