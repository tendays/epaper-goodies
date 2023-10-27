/**
 * 
 */
package org.gamboni.pi.epaper.render.widget.motd;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * @author tendays
 *
 */
public class LocalMotd implements MotdWidget.Face {

	private static final File MESSAGE_FILE = new File("/tmp/message");
	private static final Path MESSAGE_PATH = MESSAGE_FILE.toPath();

	@Override
	public Optional<MotdWidget.Message> getMotd() {
		try {
			if (MESSAGE_FILE.exists() && Files.getLastModifiedTime(MESSAGE_PATH).toInstant().isAfter(Instant.now().minus(Duration.ofHours(12)))) {
				return Optional.of(new MotdWidget.Message(
						String.join("\n", Files.readAllLines(MESSAGE_PATH)),
						MotdWidget.Priority.IMPORTANT));
			}
		} catch (IOException e) {
			// ignore and return nothing below
		}
		return Optional.empty();
	}
}
