package org.gamboni.pi.epaper.render.calendar;

import com.google.common.collect.ImmutableMap;
import org.gamboni.pi.epaper.render.CacheFile;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CineFeuillesCalendar implements CalendarLoader {

    private static final Pattern DATE = Pattern.compile(".*Lausanne *et *[a-zA-Z]* *([0-9]+) +([a-zéû]*) +à +Vevey, 18h et 20h30.*");
    private static final Map<String, Month> MONTHS = ImmutableMap.<String, Month>builder()
            .put("janvier", Month.JANUARY)
            .put("février",Month.FEBRUARY)
            .put("mars", Month.MARCH)
            .put("avril", Month.APRIL)
            .put("mai", Month.MAY)
            .put("juin", Month.JUNE)
            .put("juillet", Month.JULY)
            .put("août", Month.AUGUST)
            .put("septembre", Month.SEPTEMBER)
            .put("octobre", Month.OCTOBER)
            .put("novembre", Month.NOVEMBER)
            .put("décembre", Month.DECEMBER)
            .build();


    public static void main(String[] a) {
        try {
            new CineFeuillesCalendar().loadFromDatasource().forEach(System.out::println);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    CacheFile<List<EventInfo>> cachedEvents = new CacheFile<List<EventInfo>>(this, Duration.ofHours(3)) {
        @Override
        @SuppressWarnings("unchecked")
        protected List<EventInfo> readFromCache(InputStream input) throws Exception {
            return (List<EventInfo>) new ObjectInputStream(input).readObject();
        }

        @Override
        protected List<EventInfo> readFromDatasource() throws IOException {
            return CineFeuillesCalendar.this.loadFromDatasource();
        }
    };

    @Override
    public List<EventInfo> loadData(LocalDate from, LocalDate to) throws IOException {
        return cachedEvents.load().stream()
                .filter(eventInfo -> !eventInfo.from.isBefore(from.atStartOfDay().atZone(ZoneId.systemDefault())))
                .filter(eventInfo -> !eventInfo.from.isAfter(to.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault())))
                .collect(Collectors.toList());
    }

    private List<EventInfo> loadFromDatasource() throws IOException {
        int seasonYear = getSeasonYear();

        class EventBuilder {
            URL poster = null;
            ZonedDateTime time = null;
            String title = "Ciné-club"; // Default event title if we can't scrape the actual movie title
            private void traverse(Element elt) {
                if (elt.nodeName().equalsIgnoreCase("img") && elt.hasClass("film-poster")) {
                    //System.out.println("Found poster " + elt.attr("src"));
                    try {
                        poster = new URL(elt.attr("src"));
                    } catch (MalformedURLException e) {
                        System.err.println("Could not parse poster url");
                        e.printStackTrace();
                    }
                } else if (elt.hasClass("dates")) {
                    //System.out.println("Found dates "+ elt.wholeText());
                    Matcher parsed = DATE.matcher(elt.wholeText());
                    if (parsed.matches()) {
                        Month month = MONTHS.get(parsed.group(2));
                        if (month == null) {
                            System.err.println("Could not parse month "+ parsed.group(2));
                            return;
                        }
                        int day = Integer.parseInt(parsed.group(1));
                        int eventYear = seasonYear + (month.compareTo(Month.JUNE) > 0 ? 0 : 1);

                        time = ZonedDateTime.of(LocalDate.of(eventYear, month, day),
                                LocalTime.of(18, 0),
                                ZoneId.systemDefault());
                    } else {
                        System.err.println("Could not parse date string "+ elt.wholeText());
                    }
                } else if (elt.hasClass("film-title")) {
                    //System.out.println("Found title "+ elt.wholeText());
                    title = elt.wholeText()
                            .replaceAll(", +[A-Z][-A-Za-z]+(/[A-Z][-A-Za-z]+)*(, +[0-9]+)", "$2") // remove country information
                            .replaceAll(" +\\(v.o. ss-titrée\\)", "") // remove language information
                            .replaceAll(", +[0-9/]* +ans", "") // remove age information
                            ;
                }
                for (Element child : elt.children()) {
                    traverse(child);
                }
            }
            Optional<EventInfo> build() {
                if (time != null) { // only mandatory field
                    return Optional.of(new EventInfo(title, time, time.plusHours(2), true, Color.BLACK, poster));
                } else {
                    System.err.println("No time found - skipping film");
                    return Optional.empty();
                }
            }
        }
        List<EventInfo> result = new ArrayList<>();
        for (Element elt : Jsoup.connect("https://www.cine-feuilles.ch/cercle-d-etudes/saisons/" + seasonYear +"-"+ (seasonYear+1)).get().select("li.cercle-film-item")) {
            EventBuilder builder = new EventBuilder();
            builder.traverse(elt);
            builder.build().ifPresent(result::add);
        }
        return result;
    }

    private static int getSeasonYear() {
        var today = LocalDate.now();
        int year = today.getYear();
        if (today.getMonth().compareTo(Month.JULY) < 0) {
            year--;
        }
        return year;
    }
}
