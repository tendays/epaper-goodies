/**
 * 
 */
package org.gamboni.pi.epaper.render.constraint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * @author tendays
 *
 */
public class ConstraintSolver {
	private final List<Variable> domain = new ArrayList<>();
	private final List<Constraint> constraints = new ArrayList<>();

	public Variable addVariable() {
		Variable v = new Variable();
		this.domain.add(v);
		return v;
	}

	public void addConstraint(Constraint c) {
		this.constraints.add(c);
	}
	
	@VisibleForTesting
	Assignment newAssignment(Map<Variable, Integer> values) {
		return new Assignment(domain, values);
	}

	public Assignment solve() {
		Assignment zeroAssignment = newAssignment(ImmutableMap.of());
		if (constraints.isEmpty()) {
			return zeroAssignment;
		}
		
		Multimap<Variable, Constraint> dependencies = LinkedHashMultimap.create();
		for (Constraint c : constraints) {
			c.getDependencies().forEach(v -> dependencies.put(v, c));
		}
		Set<SolveState> currentSpace = ImmutableSet
				.of(new SolveState(dependencies, zeroAssignment, constraints));
		Set<SolveState> nextSpace = new LinkedHashSet<>();
		// int iterationNum=0; for debug logging
		while (true) {
			// System.out.println(iterationNum++ + ": "+ currentSpace.size());
			SolveState bestSolution = null;
			for (SolveState current : currentSpace) {
				nextSpace.addAll(current.checkList.get(0).refine(current));
			}
			for (SolveState next : nextSpace) {
				if (next.checkList.isEmpty()) {
					if (bestSolution == null || bestSolution.values.total > next.values.total) {
						bestSolution = next;
					}
				}
			}
			if (bestSolution != null) {
				return bestSolution.values;
			}
			currentSpace = nextSpace;
			nextSpace = new LinkedHashSet<>();
		}
	}

	protected Variable removeFirst(Set<Variable> collection) {
		Iterator<Variable> iterator = collection.iterator();
		Variable v = iterator.next();
		iterator.remove();
		return v;
	}
}
