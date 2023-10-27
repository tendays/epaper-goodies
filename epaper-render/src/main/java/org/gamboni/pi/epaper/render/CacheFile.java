/**
 * 
 */
package org.gamboni.pi.epaper.render;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * @author tendays
 *
 */
public abstract class CacheFile<T> {
	private final File cacheFile;
	private final Duration validity;

	protected CacheFile(Object owner, Duration validity) {
		this(owner.getClass(), "", validity);
	}

	protected CacheFile(Object owner, String key, Duration validity) {
		this(owner.getClass(), key, validity);
	}

	protected CacheFile(Class<?> owner, String key, Duration validity) {
		if (key.length() > 0) {
			key = "-" + key;
		}
		this.cacheFile = new File(owner.getSimpleName() + key +"-cache");
		this.validity = validity;
	}

	
	protected abstract T readFromCache(InputStream input) throws Exception;
	protected abstract T readFromDatasource() throws IOException;
	protected void writeToCache(T data, OutputStream out) throws Exception {
		try (ObjectOutputStream oos = new ObjectOutputStream(out)) {
			oos.writeObject(data);
		}
	}
	
	public T load() throws IOException {
		/* First try loading from cache if cache exists and was written today */
		if (cacheFile.exists() && Instant.ofEpochMilli(cacheFile.lastModified()).plus(validity).isAfter(Instant.now())) {
			try (InputStream cacheInput = new FileInputStream(cacheFile)) {
				return readFromCache(cacheInput);
			} catch (Exception e) {
				/* Malformed/stale cache file -> ignore and load from server */
			}
		}
		
		T data = readFromDatasource();

		/* Save data to cache before returning */
		try (OutputStream out = new FileOutputStream(cacheFile)) {
			writeToCache(data, out);
		} catch (Exception e) {
			System.err.println("Failed saving data to cache!");
			e.printStackTrace();
			// We can still go on and use the data
		}
		
		return data;
	}
}
