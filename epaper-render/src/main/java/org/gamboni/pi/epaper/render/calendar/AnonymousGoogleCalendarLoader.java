package org.gamboni.pi.epaper.render.calendar;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import org.gamboni.pi.epaper.render.CacheFile;

import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.*;
import java.util.List;
import java.util.stream.Collectors;

public class AnonymousGoogleCalendarLoader implements CalendarLoader {

    private static class AnonGoogleCalendar {
        List<GoogleCalendarItem> items;
    }

    private static class GoogleCalendarItem {
        String summary;
        GoogleDateTime start;
        GoogleDateTime end;
    }
    private static class GoogleDateTime {
        LocalDate date;
        ZonedDateTime dateTime;
        String timeZone;

        public ZonedDateTime toZonedDateTime() {
            if (date != null) {
                return date.atStartOfDay(ZoneId.systemDefault());
            } else {
                return dateTime;
            }
        }

        public boolean hasTime() {
            return dateTime != null;
        }
    }

    private final String calendarId;
    private final String calendarKey;
    private final CacheFile<List<EventInfo>> cache;

    private LocalDate from;
    private LocalDate to;

    public AnonymousGoogleCalendarLoader(String calendarId, String calendarKey) {
        this.calendarId = calendarId;
        this.calendarKey = calendarKey;
        cache = new CacheFile<>(this, calendarId, Duration.ofHours(8)) {
            @Override
            protected List<EventInfo> readFromCache(InputStream input) throws Exception {
                try (ObjectInputStream cacheInput = new ObjectInputStream(input)) {
                    LocalDate cachedFrom = (LocalDate)cacheInput.readObject();
                    LocalDate cachedTo = (LocalDate)cacheInput.readObject();
                    if (!cachedFrom.equals(from) || !cachedTo.equals(to)) {
                        throw new IllegalStateException("Cached values "+ cachedFrom +" → "+ cachedTo +" do not match requested " +
                                from +" → "+ to);
                    }
                    // Make sure each element is a valid EventInfo before returning the data
                    return ((List<?>) cacheInput.readObject()).stream()
                            .map(l -> (EventInfo) l).collect(Collectors.toList());
                }
            }

            @Override
            protected void writeToCache(List<EventInfo> data, OutputStream out) throws Exception {
                try (ObjectOutputStream oos = new ObjectOutputStream(out)) {
                    oos.writeObject(from);
                    oos.writeObject(to);
                    oos.writeObject(data);
                }
            }

            @Override
            protected List<EventInfo> readFromDatasource() throws IOException {
                var conn = (HttpURLConnection) new URL("https://clients6.google.com/calendar/v3/calendars/" + calendarId +
                        "@group.calendar.google.com/events?calendarId=" + calendarId +
                        "%40group.calendar.google.com&singleEvents=true&timeZone=Europe%2FParis&maxAttendees=1&maxResults=250&sanitizeHtml=true&timeMin=" + from +
                        "T00%3A00%3A00%2B02%3A00&timeMax=" + to.plusDays(1) + "T00%3A00%3A00%2B02%3A00&key="+ calendarKey).openConnection();
                try (var in = new InputStreamReader(conn.getInputStream())) {
                    var cal = new GsonBuilder()
                            .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>)
                                    (json, type, context) -> LocalDate.parse(json.getAsString()))
                            .registerTypeAdapter(ZonedDateTime.class, (JsonDeserializer<ZonedDateTime>)
                                    (json, type, context) -> ZonedDateTime.parse(json.getAsString()))
                            .create()
                            .fromJson(in, AnonGoogleCalendar.class);
                    return cal.items.stream().map(item -> new EventInfo(
                            item.summary,
                            item.start.toZonedDateTime(),
                            item.end.toZonedDateTime(),
                            item.start.hasTime(),
                            Color.BLACK
                    )).collect(Collectors.toList());
                }
            }
        };
    }

    /** For testing: run as an application, pass calendar id as first parameter and calendar key as second one. */
    public static void main(String[] a) {
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        try {
            for (var e : new AnonymousGoogleCalendarLoader(a[0], a[1])
                    .loadData(monthStart,
                    monthStart.plusMonths(1).minusDays(1))) {
                System.out.println(e);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    @Override
    public List<EventInfo> loadData(LocalDate from, LocalDate to) throws IOException {
        this.from = from;
        this.to = to;
        return cache.load();
    }
}
