/**
 * 
 */
package org.gamboni.pi.epaper.render.constraint;

import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * @author tendays
 *
 */
public interface Constraint {
	public static class AtLeastConst implements Constraint {
		private final Variable variable;
		private final int min;

		public AtLeastConst(Variable variable, int min) {
			this.variable = variable;
			this.min = min;
		}

		@Override
		public List<SolveState> refine(SolveState assignment) {
			int value = assignment.get(variable);
			return ImmutableList.of(assignment.put(this, variable, (value < min) ? min : value));
		}

		@Override
		public List<Variable> getDependencies() {
			return ImmutableList.of(); // a variable increasing will never invalidate this constraint
		}
		public String toString() {
			return variable +"≥"+ min;
		}
	}
	
	public static class AtLeastVar implements Constraint {
		private final Variable lower, upper;
		private final int delta; // minimal difference
		public AtLeastVar(Variable lower, Variable upper, int delta) {
			this.lower = lower;
			this.upper = upper;
			this.delta = delta;
		}
		@Override
		public List<SolveState> refine(SolveState state) {
			int min = state.get(lower) + delta;
			int value = state.get(upper);
			return ImmutableList.of(state.put(this, upper, (value < min) ? min : value));
		}
		
		@Override
		public List<Variable> getDependencies() {
			// *lower* growing can invalidate this. If upper grows it's always ok
			return ImmutableList.of(lower);
		}
		public String toString() {
			return upper +"≥"+ lower +"+"+ delta;
		}
	}
	
	public static class NoCollision implements Constraint {
		private final Variable variable;
		private final Variable reference;
		private final int from;
		private final int to;
		public NoCollision(Variable variable, Variable reference, int from, int to) {
			this.variable = variable;
			this.reference = reference;
			this.from = from;
			this.to = to;
		}
		@Override
		public List<SolveState> refine(SolveState assignment) {
			int value = assignment.get(variable);
			int refValue = assignment.get(reference);
			if ((value >= refValue + from) && (value <= refValue + to)) {
				return ImmutableList.of(
						assignment.put(this, variable, refValue + to + 1),
						assignment.put(this, reference, value - from + 1));
			} else {
				return ImmutableList.of(
						assignment.without(this));
			}
		}
		@Override
		public List<Variable> getDependencies() {
			return ImmutableList.of(variable, reference);
		}
		public String toString() {
			return variable +"∉ ["+ reference +"+"+ from +", "+ reference +"+"+ to +"]";
		}
	}
	
	List<SolveState> refine(SolveState state);
	/** All variables that may invalidate this constraint by changing. */
	List<Variable> getDependencies();
}
