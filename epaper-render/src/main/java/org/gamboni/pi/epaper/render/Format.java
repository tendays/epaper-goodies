/**
 * 
 */
package org.gamboni.pi.epaper.render;

import java.time.format.DateTimeFormatter;

/** Utility methods and constants to format text
 *
 * @author tendays
 */
public abstract class Format {
    public static final DateTimeFormatter TIME_PATTERN = DateTimeFormatter.ofPattern("HH:mm ");
}
