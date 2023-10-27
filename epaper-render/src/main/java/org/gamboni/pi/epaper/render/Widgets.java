/**
 * 
 */
package org.gamboni.pi.epaper.render;

import java.time.Month;
import java.util.List;

import org.gamboni.pi.epaper.render.calendar.*;
import org.gamboni.pi.epaper.render.gfx.layout.IntRectangle;
import org.gamboni.pi.epaper.render.weather.MeteoSuisseWeatherProvider;
import org.gamboni.pi.epaper.render.weather.WeatherDangerMotd;
import org.gamboni.pi.epaper.render.widget.motd.DayMotd;
import org.gamboni.pi.epaper.render.widget.motd.FeedMotd;
import org.gamboni.pi.epaper.render.widget.motd.LocalMotd;
import org.gamboni.pi.epaper.render.widget.motd.MotdWidget;
import org.gamboni.pi.epaper.render.widget.AccountBalanceWidget;
import org.gamboni.pi.epaper.render.widget.BigClockWidget;
import org.gamboni.pi.epaper.render.widget.CalendarWidget;
import org.gamboni.pi.epaper.render.widget.ChineseCalendarWidget;
import org.gamboni.pi.epaper.render.widget.CovidWidget;
import org.gamboni.pi.epaper.render.widget.DateTimeWidget;
import org.gamboni.pi.epaper.render.widget.ImageWidget;
import org.gamboni.pi.epaper.render.widget.NagiosWidget;
import org.gamboni.pi.epaper.render.widget.ProgressWidget;
import org.gamboni.pi.epaper.render.widget.TimetableWidget;
import org.gamboni.pi.epaper.render.widget.WeatherWidget;
import org.gamboni.pi.epaper.render.widget.WidgetFactory;

import com.google.common.collect.ImmutableList;

/**
 * @author tendays
 *
 */
public class Widgets {

	private static final List<WidgetFactory> widgetFactories = ImmutableList.of(
			/* Clock/Calendar/Motd faces */
			WidgetFactory.face("AnonGoogleCalendar", args -> new AnonymousGoogleCalendarLoader(
					args.getString(), args.getString()
			)),
			WidgetFactory.face("CalDav", args -> new CalDavLoader(args.getString(),
					args.getInt(),
					args.getString(),
					args.getString(),
					args.getString(),
					args.getList())),
			WidgetFactory.face("CineFeuilles", __ -> new CineFeuillesCalendar()),
			WidgetFactory.face("ChineseCalendar", __ -> new ChineseCalendarWidget()),
			WidgetFactory.face("DayMotd", args -> new DayMotd(Month.of(args.getInt()), args.getInt(), args.getString())),
			WidgetFactory.face("Feed", args -> new FeedMotd(args.getString(), args.getList())),
			WidgetFactory.face("GoogleCalendar", args -> new GoogleCalendarLoader(args.getString(), args.getList())),
			WidgetFactory.face("LibraryCalendar", args -> new LibraryCalendarLoader(args.getList())),
			WidgetFactory.face("LocalMotd", __ -> new LocalMotd()),
			WidgetFactory.face("TimeTable", args -> new TimetableWidget(args.getList())),
			WidgetFactory.face("TravelPassCalendar", args -> new TravelPassCalendarLoader(args.getString(), args.getRest())),
			WidgetFactory.face("WeatherDangers", args -> new WeatherDangerMotd()),
			// int zipCode
			WidgetFactory.face("MeteoSuisse", args -> new MeteoSuisseWeatherProvider(args.getInt())),

			WidgetFactory.widget("AccountBalance", (__, args) -> new AccountBalanceWidget(args.getString())),
			WidgetFactory.widget("BigClock", (layout, ___) -> {
				BigClockWidget w = new BigClockWidget();
				layout.getClockFaces(w::add);
				return w;
			}),
			WidgetFactory.widget("Calendar", (layout, args) -> new CalendarWidget(args.getInt(), layout.getCalendars())),
			WidgetFactory.widget("Covid", (__, ___) -> new CovidWidget()),
			WidgetFactory.widget("DateTime", (__, ___) -> new DateTimeWidget()),
			WidgetFactory.widget("Image", (__, args) -> new ImageWidget(args.getString(), false)),
			WidgetFactory.widget("Motd", (layout, __) -> new MotdWidget(layout.getMotds())),
			WidgetFactory.widget("Nagios", (__, args) ->
			// String cgiUrl, String host, String service, List<String> db, String rrdopts
			new NagiosWidget(
					args.getString(),
					args.getString(),
					args.getString(),
					args.getList(),
					args.getRest())),
			WidgetFactory.widget("Progress", (__, ___) -> new ProgressWidget()),
			WidgetFactory.widget("Weather", (config, ___) -> new WeatherWidget(config))
			);

	public static boolean parse(Layout layout, IntRectangle rect, String line) {
		for (WidgetFactory factory : widgetFactories) {
			if (factory.parse(layout, rect, new LineParser(line))) {
				return true;
			}
		}
		return false;
	}
}
