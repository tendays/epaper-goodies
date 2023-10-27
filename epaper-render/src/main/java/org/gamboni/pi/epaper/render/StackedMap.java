/**
 * 
 */
package org.gamboni.pi.epaper.render;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;

/**
 * @author tendays
 *
 */
public class StackedMap<K, V> extends AbstractMap<K, V> {
	private final Map<K, V> base;
	private final Map<K, V> overlay;

	public StackedMap(Map<K, V> base, Map<K, V> overlay) {
		this.base = base;
		this.overlay = overlay;
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return new AbstractSet<Entry<K, V>>() {

			@Override
			public Iterator<Entry<K, V>> iterator() {
				// return full overlay, and base entries that are not hidden by overlay
				return Iterators.concat(overlay.entrySet().iterator(),
						visibleBaseEntries());
			}

			private UnmodifiableIterator<Entry<K, V>> visibleBaseEntries() {
				return Iterators.filter(base.entrySet().iterator(),
						baseEntry -> !overlay.containsKey(baseEntry.getKey()));
			}

			@Override
			public int size() {
				return overlay.size() + Iterators.size(visibleBaseEntries());
			}
		};
	}
}
