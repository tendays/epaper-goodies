/**
 * 
 */
package org.gamboni.pi.epaper.render.constraint;

import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

/** A point in the search space of the constraint solver.
 *
 * @author tendays
 */
public class SolveState {
	private final ImmutableMultimap<Variable, Constraint> dependencyMap;
	public final Assignment values;
	public final ImmutableList<Constraint> checkList;
	private final int hash;

	public SolveState(Multimap<Variable, Constraint> dependencyMap, Assignment values, List<Constraint> checkList) {
		this.dependencyMap = ImmutableMultimap.copyOf(dependencyMap);
		this.values = values;
		this.checkList = ImmutableList.copyOf(checkList);
		
		this.hash = Objects.hash(values, checkList);
	}

	public int get(Variable variable) {
		return values.get(variable);
	}
	
	SolveState put(Constraint solvedConstraint, Variable variable, int newValue) {
		int oldValue = values.get(variable);
		ImmutableList.Builder<Constraint> newCheckList = dropConstraint(solvedConstraint);
		if (oldValue == newValue) {
			return new SolveState(dependencyMap, values, newCheckList.build());
		} else {
			for (Constraint c : dependencyMap.get(variable)) {
				if (c != solvedConstraint && !this.checkList.contains(c)) {
					newCheckList.add(c);
				}
			}
			return new SolveState(dependencyMap, values.put(variable, newValue), newCheckList.build());
		}
	}

	private ImmutableList.Builder<Constraint> dropConstraint(Constraint solvedConstraint) {
		ImmutableList.Builder<Constraint> newCheckList = ImmutableList.builder();
		for (Constraint c : this.checkList) {
			if (c != solvedConstraint) {
				newCheckList.add(c);
			}
		}
		return newCheckList;
	}

	public SolveState without(Constraint solvedConstraint) {
		return new SolveState(dependencyMap, values, dropConstraint(solvedConstraint).build());
	}
	
	public int hashCode() {
		return this.hash;
	}
	
	public boolean equals(Object that) {
		return (that instanceof SolveState) &&
				((SolveState)that).checkList.equals(this.checkList) &&
				((SolveState)that).values.equals(this.values);
	}
	
	public String toString() {
		return "SolveState("+ this.values.total +")";
	}
}
