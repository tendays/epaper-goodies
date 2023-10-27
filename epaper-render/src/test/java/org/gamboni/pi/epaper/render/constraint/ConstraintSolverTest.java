/**
 * 
 */
package org.gamboni.pi.epaper.render.constraint;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

/**
 * @author tendays
 *
 */
class ConstraintSolverTest {

	@Test
	void twoConstraints() {
		ConstraintSolver cs = new ConstraintSolver();
		Variable x = cs.addVariable();
		Variable y = cs.addVariable();
		cs.addConstraint(new Constraint.AtLeastVar(x, y, 2));
		cs.addConstraint(new Constraint.AtLeastConst(x, 1));
		
		Assignment actual = cs.solve();
		
		Assertions.assertEquals(cs.newAssignment(ImmutableMap.of(x, 1, y, 3)), actual);
	}

	@Test
	void collisionAvoidanceTest() {
		ConstraintSolver cs = new ConstraintSolver();
		Variable x = cs.addVariable();
		Variable y = cs.addVariable();
		Variable z = cs.addVariable();
		Variable t = cs.addVariable();
		cs.addConstraint(new Constraint.NoCollision(x, y, 0, 5));
		cs.addConstraint(new Constraint.NoCollision(y, z, 0, 5));
		cs.addConstraint(new Constraint.NoCollision(z, t, 0, 5));
		
		Assignment actual = cs.solve();
		
		Assertions.assertEquals(cs.newAssignment(ImmutableMap.of(x, 0, y, 1, z, 2, t, 3)), actual);
	}

	@Test
	void symCollisionAvoidanceTest() {
		ConstraintSolver cs = new ConstraintSolver();
		Variable x = cs.addVariable();
		Variable y = cs.addVariable();
		Variable z = cs.addVariable();
		Variable t = cs.addVariable();
		/* Each variable must be more than 5 units away from its neighbours */
		cs.addConstraint(new Constraint.NoCollision(x, y, 0, 5));
		cs.addConstraint(new Constraint.NoCollision(y, x, 0, 5));
		
		cs.addConstraint(new Constraint.NoCollision(y, z, 0, 5));
		cs.addConstraint(new Constraint.NoCollision(z, y, 0, 5));
		
		cs.addConstraint(new Constraint.NoCollision(z, t, 0, 5));
		cs.addConstraint(new Constraint.NoCollision(t, z, 0, 5));
		
		Assignment actual = cs.solve();
		
		// we don't actually care if it's 0-6-0-6 or 6-0-6-0
		Assertions.assertEquals(cs.newAssignment(ImmutableMap.of(x, 0, y, 6, z, 0, t, 6)).total, actual.total);
	}
}
