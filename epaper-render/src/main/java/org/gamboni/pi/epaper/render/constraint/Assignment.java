/**
 * 
 */
package org.gamboni.pi.epaper.render.constraint;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;

/**
 * @author tendays
 *
 */
public class Assignment {
	private final List<Variable> domain;
	private final Assignment parent;
	private final ImmutableMap<Variable, Integer> values;
	public final int total;
	
	public Assignment(List<Variable> domain, Map<Variable, Integer> values) {
		this.domain = domain;
		
		this.parent = null;
		this.values = ImmutableMap.copyOf(values);
		this.total = values.values().stream().collect(Collectors.summingInt(Integer::intValue));
	}

	private Assignment(Assignment parent, Map<Variable, Integer> values) {
		this.domain = parent.domain;
		this.parent = parent;
		this.values = ImmutableMap.copyOf(values);
		int total = parent.total;
		for (Map.Entry<Variable, Integer> entry : values.entrySet()) {
			total += entry.getValue() - parent.get(entry.getKey());
		}
		this.total = total;
	}
	
	@Override
	public int hashCode() {
		return total;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Assignment)) { return false; }
		Assignment that = (Assignment) obj;
		if (that.domain != this.domain) { return false; }
		if (that.total != this.total) { return false; }
		for (Variable v : domain) {
			if (this.get(v) != that.get(v)) {
				return false;
			}
		}
		return true;
	}

	public int get(Variable v) {
		Integer value = values.get(v);
		if (value != null) {
			return value;
		} else if (parent != null) {
			return parent.get(v);
		} else {
			return 0;
		}
	}
	public Assignment put(Variable v, int value) {
		if (values.size() == 1 && values.containsKey(v)) {
			// optimisation
			return new Assignment(parent, ImmutableMap.of(v, value));
		} else {
			return new Assignment(this, ImmutableMap.of(v, value));
		}
	}
	
	public String toString() {
		return total + ":[" +
				domain.stream()
						.map(v -> v + "=" + get(v))
						.collect(Collectors.joining("; "))
				+ "]";
	}
}
