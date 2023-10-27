/**
 * 
 */
package org.gamboni.pi.epaper.render.gfx.layout;

import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

/**
 * @author tendays
 *
 */
public abstract class AbstractPolarElement implements LayoutElement, Serializable {

	private static final long serialVersionUID = -7621396214879176712L;

	public static class Bounds implements Serializable {
		private static final long serialVersionUID = -6794357123338463955L;
		public final double min;
		public final double max;
		
		public Bounds(double min, double max) {
			this.min = min;
			this.max = max;
		}
	}
	
	public enum LayoutParameter {
		/** Distance of anchor point from origin */
		RADIUS,
		/** Polar angle coordinate of the anchor. Zero is at twelve o'clock, increases clockwise, one circle is 2pi */
		DIRECTION,
		/** Maximal width of the text block */
		WIDTH,
		/** Position of the text's bounding box relative to the anchor. This value is understood in modulo 4.
		 * <ul><li>0 puts the bottom-right angle at the anchor
		 * <li>increasing values make the text shift rightwards, until...
		 * <li> 1 that puts the bottom-left angle at the anchor.
		 * <li>Similarly, between 1 and 2 the text shifts downwards with
		 * <li>2 having the top-left angle at the anchor.
		 * <li>3 has the top-right angle at the anchor and
		 * <li>4 is back to bottom-right.
		 * </ul> 
		 */
		ANCHOR
	}

	public static class Dwa {
		private Parameters p;

		private Dwa(Parameters p) {
			this.p = p;
		}

		public Wa direction(double exact) {
			return this.direction(exact, exact);
		}

		public Wa direction(double min, double max) {
			p.bounds.put(LayoutParameter.DIRECTION, new Bounds(min, max));
			return new Wa(p);
		}
	}
	
	public static class Wa {
		private Parameters p;

		private Wa(Parameters p) {
			this.p = p;
		}
		public A width(double exact) {
			return this.width(exact, exact);
		}
		public A width(double min, double max) {
			p.bounds.put(LayoutParameter.WIDTH, new Bounds(min, max));
			return new A(p);
		}
		public ImmutableMap<LayoutParameter, Bounds> getBounds() {
			// TODO should make these immutable so we know what's in bounds
			return ImmutableMap.copyOf(p.bounds);
		}
	}

	public static class A {
		private Parameters p;
		private A(Parameters p) {
			this.p = p;
		}
		
		public Parameters anchor(double min, double max) {
			p.bounds.put(LayoutParameter.ANCHOR, new Bounds(min, max));
			return this.p;
		}
	}
	
	public static class Parameters {
		Map<LayoutParameter, Bounds> bounds = new HashMap<>();
		
		public static Dwa radius(double exact) {
			return radius(exact, exact);
		}
		
		public static Dwa radius(double min, double max) {
			Parameters p = new Parameters();
			p.bounds.put(LayoutParameter.RADIUS, new Bounds(min, max));
			return new Dwa(p);
		}
	}

	public final ImmutableMap<LayoutParameter, Bounds> bounds;
	
	protected AbstractPolarElement(Map<LayoutParameter, Bounds> bounds) {
		this.bounds = ImmutableMap.copyOf(bounds);
	}
	
	@Override
	public ImmutableMap<LayoutParameter, Bounds> getBounds() {
		return this.bounds;
	}

	@Override
	public Map<LayoutParameter, Force> computeSlope(Map<LayoutParameter, Double> params, Rectangle2D boundingBox,
			Corner corner) {
		double radius = params.get(LayoutParameter.RADIUS);
		double direction = params.get(LayoutParameter.DIRECTION);
		
		Map<LayoutParameter, Force> result = new HashMap<>();
		result.put(LayoutParameter.RADIUS, new Force(Math.sin(direction), -Math.cos(direction)));
		result.put(LayoutParameter.DIRECTION, new Force(Math.cos(direction)*radius, Math.sin(direction)*radius));
		
		return result;
	}

	protected PolarCoords getAnchorCoords(Map<LayoutParameter, Double> values) {
		PolarCoords anchorCoords = new PolarCoords(values.get(LayoutParameter.RADIUS), values.get(LayoutParameter.DIRECTION));
		return anchorCoords;
	}
}
