package org.gamboni.pi.watchy;

import com.google.common.base.Splitter;
import com.google.common.collect.Streams;
import org.gamboni.pi.epaper.render.calendar.EventInfo;
import org.gamboni.pi.epaper.render.weather.WeatherProvider;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class WatchyData {
    public List<WatchyEvent> calendar;
    public List<WeatherWithTime> weather;

    public static class WatchyEvent {
        public String text;
        public long time;

        public WatchyEvent(EventInfo ev) {
                this.text =
                        Streams.stream(Splitter.on(' ').split(ev.text))
                                .limit(4)
                                .map(s -> s.substring(0, 1))
                                .collect(Collectors.joining());
                this.time = ev.from.toEpochSecond();
        }
    }

    public static class WeatherWithTime extends WeatherProvider.WeatherPoint {
        public long time;

        public WeatherWithTime(ZonedDateTime time, WeatherProvider.WeatherPoint data) {
            super(data);
            this.time = time.toEpochSecond();
        }
    }
}
