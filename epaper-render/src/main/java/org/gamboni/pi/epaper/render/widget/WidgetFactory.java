/**
 * 
 */
package org.gamboni.pi.epaper.render.widget;

import java.util.function.Function;

import org.gamboni.pi.epaper.render.Layout;
import org.gamboni.pi.epaper.render.LineParser;
import org.gamboni.pi.epaper.render.gfx.layout.IntRectangle;
import org.gamboni.pi.epaper.render.calendar.CalendarLoader;
import org.gamboni.pi.epaper.render.weather.WeatherProvider;
import org.gamboni.pi.epaper.render.widget.motd.MotdWidget;

/**
 * @author tendays
 *
 */
public interface WidgetFactory {
	boolean parse(Layout layout, IntRectangle rect, LineParser line);
	
	interface AreaBased {
		Widget parse(Layout layout, LineParser line);
	}

	static WidgetFactory widget(String name, AreaBased factory) {
		return (layout, rect, line) -> {
			
			if (line.tryConsume(name)) {
				Widget item = factory.parse(layout, line);
				if (rect != null) { layout.add(item, rect); }
				addFaces(layout, item);
				return true;
			} else {
				return false;
			}
		};
	}
	
	static WidgetFactory face(String name, Function<LineParser, ?> factory) {
		return (layout, __, line) -> {
			if (line.tryConsume(name)) {
				Object item = factory.apply(line);
				boolean match = addFaces(layout, item);
				if (!match) {
					throw new IllegalArgumentException(item.getClass().getSimpleName());
				}
				return true;
			} else {
				return false;
			}
		};
	}

	static boolean addFaces(Layout layout, Object item) {
		boolean match = false;
		if (item instanceof CalendarLoader) {
			layout.add((CalendarLoader)item);
			match = true;
		}
		if (item instanceof ClockFace) {
			layout.add((ClockFace)item);
			match = true;
		}
		if (item instanceof MotdWidget.Face) {
			layout.add((MotdWidget.Face)item);
			match = true;
		}
		if (item instanceof WeatherProvider) {
			layout.setWeather((WeatherProvider) item);
			match = true;
		}
		return match;
	}
}
