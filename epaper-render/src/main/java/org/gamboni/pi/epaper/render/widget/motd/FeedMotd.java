package org.gamboni.pi.epaper.render.widget.motd;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.gamboni.pi.epaper.render.Html;

import com.google.common.base.Charsets;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;

public class FeedMotd implements MotdWidget.Face {

	private final String url;
	private final List<String> watchList;
	
	public FeedMotd(String url, List<String> watchList) {
		this.url = url;
		this.watchList = watchList;
	}
	
	@Override
	public Optional<MotdWidget.Message> getMotd() {
		try {
			SyndFeedInput feedInput = new SyndFeedInput();
			SyndFeed feed = feedInput.build(new InputStreamReader(new URL(url).openStream(),
					Charsets.UTF_8));

			return feed.getEntries().stream()
					.filter(e -> e.getPublishedDate().after(Date.from(Instant.now().minus(Duration.ofDays(2)))))
					.max(Comparator.comparingInt((SyndEntry entry) -> {
						String text = entry.getTitle() + formatContents(entry);
						int index = 0;
						for (String pattern : watchList) {
							if (text.toLowerCase().contains(pattern.toLowerCase())) {
								return index;
							}
							index--;
						}
						return Integer.MIN_VALUE;
					}).thenComparing(SyndEntry::getPublishedDate))
					.flatMap(e -> MotdWidget.Message.of(formatEntry(e), MotdWidget.Priority.DEFAULT));
		} catch (IOException | IllegalArgumentException | FeedException io) {
			return MotdWidget.Message.of(io.toString(), MotdWidget.Priority.IMPORTANT);
		}
	}

	private String formatEntry(SyndEntry entry) {
		String contents = formatContents(entry);
		StringBuilder result = new StringBuilder(LocalDate.from(entry.getPublishedDate().toInstant().atZone(ZoneId.systemDefault())) + "\n");
		if (!contents.contains(entry.getTitle().trim())) {
			result.append(entry.getTitle().trim() +"\n");
		}
		result.append(contents);
		return result.toString();
	}

	public String formatContents(SyndEntry entry) {
		return entry.getContents().stream().map(e -> Html.extractText(e.getValue()))
		.collect(Collectors.joining("; ")).trim();
	}
}
