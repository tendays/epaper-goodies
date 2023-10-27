/**
 * 
 */
package org.gamboni.pi.epaper.render.calendar;

import java.awt.Color;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

/**
 * @author tendays
 *
 */
public class GoogleCalendarLoader implements CalendarLoader {

	private static final String APPLICATION_NAME = "Raspberry";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR_READONLY);

    private final String credentials;
	private final List<String> calendars;
    
    public GoogleCalendarLoader(String credentials, List<String> calendars) {
    	this.credentials = credentials;
    	this.calendars = calendars;
    }
    
    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = new FileInputStream(credentials);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
    
    /*
	public static void main(String[] a) {
		try {
			System.out.println(new GoogleCalendarLoader().loadData());
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}*/
    
    @Override
	public List<EventInfo> loadData(LocalDate from, LocalDate to) throws IOException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = newHttpTransport();
        Calendar service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        DateTime now = new DateTime(System.currentTimeMillis());
        
        return service.calendarList().list().execute().getItems().stream().filter(c -> calendars.contains(c.getId())).flatMap(calendar ->
        {
			try {
				return service.events().list(calendar.getId())
				        .setMaxResults(10)
				        .setTimeMin(now) // TODO use from
				        .setOrderBy("startTime")
				        .setSingleEvents(true)
				        .execute().getItems().stream();
			} catch (IOException e) {
				e.printStackTrace();
				return Stream.<Event>of();
			}
		})
        .map(event -> {
                DateTime start = toDateTime(event.getStart());
                DateTime end = toDateTime(event.getEnd());
				return new EventInfo(event.getSummary(), toZonedDateTime(start), toZonedDateTime(end), !start.isDateOnly(), Color.BLACK);
            })
        .sorted()
        .collect(Collectors.toList());
    }

	protected DateTime toDateTime(EventDateTime eventDateTime) {
		DateTime start = eventDateTime.getDateTime();
		if (start == null) {
		    start = eventDateTime.getDate();
		}
		return start;
	}

	public NetHttpTransport newHttpTransport() throws IOException {
		try {
			return GoogleNetHttpTransport.newTrustedTransport();
		} catch (GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
	}
	
	private ZonedDateTime toZonedDateTime(DateTime googleDateTime) {
		return Instant.ofEpochMilli(googleDateTime.getValue()).atZone(ZoneId.systemDefault());
	}

}
