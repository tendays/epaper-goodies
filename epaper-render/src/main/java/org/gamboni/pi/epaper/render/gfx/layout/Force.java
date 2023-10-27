/**
 * 
 */
package org.gamboni.pi.epaper.render.gfx.layout;

/**
 * @author tendays
 *
 */
public class Force {
	public static final Force ZERO = new Force(0, 0);
	
	public final double x;
	public final double y;
	
	public Force(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public Force(Direction key, double value) {
		this.x = key.x * value;
		this.y = key.y * value;
	}

	public Force push(Direction forceDirection) {
		return new Force(this.x + forceDirection.x,
				this.y + forceDirection.y);
	}

	/** Return a Force that is as strong as the strongest of this and that (working in both coordinates independently).
	 * The sign of the returned force will be that of the strongest Force. */
	public Force makeAtLeast(Force that) {
		return new Force(
				Math.abs(this.x) > Math.abs(that.x) ? this.x : that.x,
						Math.abs(this.y) > Math.abs(that.y) ? this.y : that.y);
	}

	public Force opposite() {
		return new Force(-this.x, -this.y);
	}

	/** Return the scalar product of this and that Force. */
	public double scalar(Force that) {
		return this.x * that.x + this.y * that.y;
	}

	public double getIntensity() {
		return Math.hypot(x, y);
	}
	
	public String toString() {
		return getClass().getSimpleName() +"["+ x +", "+ y +"]";
	}
}
