/**
 * 
 */
package org.gamboni.pi.epaper.render.calendar;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * @author tendays
 *
 */
public interface CalendarLoader {

	List<EventInfo> loadData(LocalDate from, LocalDate to) throws IOException;

}
