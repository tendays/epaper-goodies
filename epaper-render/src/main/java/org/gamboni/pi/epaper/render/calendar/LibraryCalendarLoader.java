/**
 * 
 */
package org.gamboni.pi.epaper.render.calendar;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.gamboni.pi.epaper.render.CacheFile;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;

/**
 * @author tendays
 *
 */
public class LibraryCalendarLoader implements CalendarLoader {

	public static void main(String[] a) {
		try {
			System.out.println(Joiner.on("\n").join(LibraryCalendarLoader.loadData(a[0], a[1])));
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private List<String> accounts;
	
	public LibraryCalendarLoader(List<String> accounts) {
		this.accounts = accounts;
	}

	private static final Splitter CSV = Splitter.on(';').trimResults();
	
	private final CacheFile<List<EventInfo>> cache = new CacheFile<List<EventInfo>>(this, Duration.ofHours(4)) {

		@Override
		protected List<EventInfo> readFromCache(InputStream input) throws Exception {
			try (ObjectInputStream cacheInput = new ObjectInputStream(input)) {
				// Make sure each element is a valid EventInfo before returning the data
				return ((List<?>) cacheInput.readObject()).stream()
						.map(l -> (EventInfo) l).collect(Collectors.toList());
			}
		}

		@Override
		protected List<EventInfo> readFromDatasource() throws IOException {
			return accounts.stream()
				.flatMap(account -> {
					int space = account.indexOf(' ');
					try {
					return loadData(account.substring(0, space),
							account.substring(space+1))
							.stream();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				})
					.sorted()
					.collect(Collectors.toList());
		}
	};
	
	@Override
	public List<EventInfo> loadData(LocalDate from, LocalDate to) throws IOException {
		return cache.load();
	}

	public static List<EventInfo> loadData(String username, String password) throws IOException {
		HttpURLConnection logonConnection = (HttpURLConnection)new URL("https://biblio.vevey.ch/Default/Portal/Recherche/logon.svc/logon").openConnection();
		logonConnection.setRequestMethod("POST");
		logonConnection.setDoOutput(true);
		
		byte[] postData = ImmutableMap.of(
				"username", username,
				"password", password)
				.entrySet()
				.stream()
				.map(entry -> urlEncode(entry.getKey()) + "=" 
				         + urlEncode(entry.getValue()))
				.collect(Collectors.joining("&"))
				.getBytes(StandardCharsets.UTF_8);
		
		logonConnection.setFixedLengthStreamingMode(postData.length);
		logonConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		logonConnection.connect();
		try(OutputStream os = logonConnection.getOutputStream()) {
		    os.write(postData);
		}
		
		List<String> cookies = new ArrayList<>();
		for (Map.Entry<String, List<String>> header : logonConnection.getHeaderFields().entrySet()) {
			// http response line with status code is a "header" without key
			if (header.getKey() != null && header.getKey().equalsIgnoreCase("Set-Cookie")) {
				for (String cookie : header.getValue()) {
					cookies.add(Splitter.on(';').splitToList(cookie).get(0));
				}
			}
		}
		
		HttpURLConnection listConnection = (HttpURLConnection) new URL("https://biblio.vevey.ch/Default/Portal/Services/ilsclient.svc/ExportAccount?type=csv&sections=loans&token="+
				System.currentTimeMillis() +"&userUniqueIdentifiers=current").openConnection();
		
		listConnection.setRequestProperty("Cookie", Joiner.on("; ").join(cookies));
		
		Map<LocalDate, Multiset<String>> titlesByDay = new TreeMap<>();
		
		// debug: print response headers listConnection.getHeaderFields().forEach((k, v) -> System.err.println(k +" = "+ v));
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(listConnection.getInputStream(), StandardCharsets.UTF_8))) {
			List<String> head = CSV.splitToList(unbom(reader.readLine()));
			int titre = head.indexOf("Titre");
			int date = head.indexOf("Date de retour prévue");
			if (titre == -1 || date == -1) {
				ZonedDateTime now = LocalDateTime.now().atZone(ZoneId.systemDefault());
				return ImmutableList.of(new EventInfo("Error parsing library response", now, now.plusHours(1), false, Color.BLACK));
			}
			String line;
			while ((line = reader.readLine()) != null) {
				List<String> record = Lists.transform(CSV.splitToList(line), c -> unquote(c));
				titlesByDay.computeIfAbsent(LocalDate.parse(record.get(date),
						DateTimeFormatter.ofPattern("dd/MM/yyyy")),
						__ -> TreeMultiset.create())
				.add(record.get(titre));
			}
		}
		
		List<EventInfo> result = new ArrayList<>();
		
		class State {
			LocalDate day;
			/** The current group of items with similar titles */
			Multiset<String> group = TreeMultiset.create();
			/** The common prefix in the current group (or null if the group is empty). */
			String prefix = null;
			String comma;
			
			void prepare(LocalDate day) {
				this.day = day;
				group.clear();
				prefix = null;
			}
			
			void process(String title) {
				title = title.trim();
				if (prefix == null) {
					prefix = title;
					group.add(title);
				} else {
					String common = commonPrefix(prefix, title);
					if (ImmutableSet.of("", "L'", "Le", "La", "Les").contains(common)) {
						flush();
						process(title);
					} else {
						prefix = common;
						group.add(title);
					}
				}
			}

			protected String commonPrefix(String prefix, String title) {
				String common = Strings.commonPrefix(prefix, title).trim();
				int commonLength = common.length();
				if (cutsWord(prefix, commonLength) || cutsWord(title, commonLength)) {
					int space = common.lastIndexOf(' ');
					if (space == -1) {
						common = "";
					} else {
						common = common.substring(0, space);
					}
				}
				return common;
			}

			protected boolean cutsWord(String string, int index) {
				return index < string.length() && string.charAt(index) != ' ';
			}
			
			void flush() {
				comma = prefix + " ";
				StringBuilder result = new StringBuilder();
				group.entrySet().forEach(item -> {
					String suffix = item.getElement().substring(prefix.length()).trim();
					if (suffix.isEmpty()) {
						emit(day, item.getElement() + numberTag(item));
					} else {
						result.append(comma);
						result.append(suffix + numberTag(item));
						comma = ", ";
					}
				});
				if (!result.toString().isEmpty()) {
					emit(day, result.toString());
				}

				prepare(day);
			}
			String numberTag(Multiset.Entry<String> entry) {
				return entry.getCount() > 1 ? " (×"+ entry.getCount() +")" : "";
			}
			void emit(LocalDate day, String text) {
				result.add(new EventInfo(text, day.atStartOfDay(ZoneId.systemDefault()),
						day.plusDays(1).atStartOfDay(ZoneId.systemDefault()), /*hasTime*/false, Color.BLACK));
			}
		}
		State state = new State();
		titlesByDay.forEach((day, titles) -> {
			state.prepare(day);
			titles.forEach(state::process);
			state.flush();
		});
		return result;
	}
	
	private static String unbom(String s) {
		if (s.length() > 0 && s.charAt(0) == '\uFEFF') {
			return s.substring(1);
		} else {
			return s;
		}
	}
	
	private static String unquote(String s) {
		if (s.startsWith("\"") && s.endsWith("\"")) {
			return s.substring(1, s.length() - 1);
		} else {
			return s;
		}
	}

	public static String urlEncode(String text) {
		try {
			return URLEncoder.encode(text, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
