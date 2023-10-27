/**
 * 
 */
package org.gamboni.pi.epaper.render.constraint;

/**
 * @author tendays
 *
 */
public class Variable implements Comparable<Variable> {
	private static int counter = 0;
	private final int id = (counter++);

	@Override
	public int compareTo(Variable that) {
		return that.id - this.id;
	}
	
	public String toString() {
		return "$"+ id;
	}
}
