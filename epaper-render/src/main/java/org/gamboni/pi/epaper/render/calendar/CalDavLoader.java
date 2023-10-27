/**
 * 
 */
package org.gamboni.pi.epaper.render.calendar;

import java.awt.Color;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.osaf.caldav4j.CalDAVCollection;
import org.osaf.caldav4j.CalDAVConstants;
import org.osaf.caldav4j.exceptions.CalDAV4JException;
import org.osaf.caldav4j.methods.CalDAV4JMethodFactory;
import org.osaf.caldav4j.methods.HttpClient;
import org.osaf.caldav4j.model.request.CalendarData;
import org.osaf.caldav4j.model.request.CalendarQuery;
import org.osaf.caldav4j.util.GenerateQuery;

import com.google.common.collect.ImmutableList;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DateProperty;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.util.CompatibilityHints;

/**
 * @author tendays
 *
 */
public class CalDavLoader implements CalendarLoader {

	static {
		CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING, true);
	}

	private final String host;
	private final int port;
	private final String protocol;
	private final String user;
	private final String password;
	private final ImmutableList<ColouredCalendar> calendars;

	private static class ColouredCalendar {
		final String path;
		final Color color;

		ColouredCalendar(String path, Color color) {
			this.path = path;
			this.color = color;
		}
	}

	public CalDavLoader(String host, int port, String protocol, String user, String password, List<String> calendars) {
		this.host = host;
		this.port = port;
		this.protocol = protocol;
		this.user = user;
		this.password = password;
		this.calendars = calendars.stream().map(s -> {
			int colon = s.indexOf(':');
			if (colon == -1) {
				return new ColouredCalendar(s, Color.BLACK);
			} else {
				return new ColouredCalendar(s.substring(0, colon), new Color(Integer.parseInt(s.substring(colon + 1), 16)));
			}
		}).collect(ImmutableList.toImmutableList());
	}

	@Override
	public List<EventInfo> loadData(LocalDate from, LocalDate to) throws IOException {
		try {
			return calDavLoad(from, to);
		} catch (CalDAV4JException e) {
			throw new IOException(e);
		}
	}

/*	public static void main(String[] a) {

		try {
			new CalDavLoader().calDavLoad();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
*/
private List<EventInfo> calDavLoad(LocalDate from, LocalDate to) throws CalDAV4JException {
	HttpClient httpClient = new HttpClient();
	httpClient.getHostConfiguration().setHost(host, port, protocol);
	UsernamePasswordCredentials httpCredentials = new UsernamePasswordCredentials(user, password);
	httpClient.getState().setCredentials(AuthScope.ANY, httpCredentials);
	httpClient.getParams().setAuthenticationPreemptive(true);

	ZonedDateTime fromInstant = atStartOfDay(from);
	ZonedDateTime toInstant = atStartOfDay(to);
	List<EventInfo> result = new ArrayList<>();
	for (ColouredCalendar calendarName : calendars) {
		CalDAVCollection collection = new CalDAVCollection(
				"/davical/caldav.php/" + calendarName.path,
				(HostConfiguration) httpClient.getHostConfiguration().clone(),
				new CalDAV4JMethodFactory(),
				CalDAVConstants.PROC_ID_DEFAULT);

		GenerateQuery gq = new GenerateQuery();
		gq.setFilter("VEVENT ["
				+ format(fromInstant) +";"
				+ format(toInstant) +"] : STATUS!=CANCELLED");
		gq.setRecurrenceSet(format(fromInstant), format(toInstant), CalendarData.EXPAND);
		CalendarQuery calendarQuery = gq.generate();
		// Patch allprop being in wrong location, causing recurrent events not to be expanded
		// Source: https://gitlab.com/davical-project/davical/-/issues/239
		@SuppressWarnings("unchecked")
		Collection<DavPropertyName> propChildren = (Collection<DavPropertyName>)calendarQuery.getProperties().getChildren();
		propChildren.removeIf(propChild -> propChild.getName().equals("allprop"));
		calendarQuery.getCalendarDataProp().getComp().addProp("allprop");
		List<Calendar> calendars = collection.queryCalendars(httpClient, calendarQuery);

		for (Calendar calendar : calendars) {
			if (calendar == null) { continue; }
			ComponentList<VEvent> componentList = calendar.getComponents().getComponents(Component.VEVENT);
			Iterator<VEvent> eventIterator = componentList.iterator();
			while (eventIterator.hasNext()) {
				VEvent ve = eventIterator.next();
				DateProperty startDate = ve.getStartDate();
				Parameter startDateValue = startDate.getParameter("value");
				boolean hasTime = startDateValue == null || !startDateValue.getValue().equals("DATE");
				/* Some events are wrongly reported as in UTC time zone, they're really in local time.
				 * Trying some heuristics for catching those events...
				 */
				boolean localTimeHack = hasTime && ve.getLastModified() != null &&
						ve.getProperty("X-MICROSOFT-CDO-BUSYSTATUS") != null &&
						ve.getProperty("CLASS") != null &&
						ve.getLastModified().getDateTime().toInstant().equals(ve.getDateStamp().getDateTime().toInstant()) &&
						ve.getCreated() != null;
				ZonedDateTime eventDate = toZonedDateTime(startDate, localTimeHack);
				if (eventDate.isBefore(toInstant) && !eventDate.isBefore(fromInstant)) {
					result.add(new EventInfo(format(ve.getSummary()) +
							(localTimeHack ? " <*>" : ""),
							eventDate,
							toZonedDateTime(ve.getEndDate(), localTimeHack),
							hasTime,
							calendarName.color));
					System.out.println(calendar.getProductId());
					System.out.println(ve);
					if (localTimeHack) {
						System.out.println("Applied local time hack");
					}
					System.out.println();
				}
			}
		}
	}
	return result;
}

	protected ZonedDateTime toZonedDateTime(DateProperty startDate, boolean localTimeHack) {
		Instant instant = startDate.getDate().toInstant();
		if (localTimeHack) {
			return instant.atZone(ZoneOffset.UTC).toLocalDateTime().atZone(ZoneId.systemDefault());
		} else {
			return instant.atZone(ZoneId.systemDefault());
		}
	}

	private String format(Summary summary) {
		return (summary == null) ? "<no summary>" : summary.getValue();
	}

	private static ZonedDateTime atStartOfDay(LocalDate date) {
		return date.atStartOfDay().atZone(ZoneId.systemDefault());
	}

	public String format(ZonedDateTime date) {
		return DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssZ").format(
				date
		);
	}
}
