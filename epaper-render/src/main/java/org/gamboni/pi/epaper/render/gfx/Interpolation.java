/**
 * 
 */
package org.gamboni.pi.epaper.render.gfx;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * @author tendays
 *
 */
public class Interpolation {
	
	private final List<Polynomial> segments;
	
	private interface Polynomial {
		double evaluate(double x);
	}
	
	/** Constant polynomial */
	private static Polynomial poly(double a) {
		return x -> a;
	}

	/** First degree polynomial */
	private static Polynomial poly(double a, double b) {
		return x -> a*x + b;
	}

	/** Second degree polynomial */
	private static Polynomial poly(double a, double b, double c) {
		return x -> a*x*x + b*x + c;
	}


	/** Third degree polynomial */
	private static Polynomial poly(double a, double b, double c, double d) {
		return x -> a*x*x*x + b*x*x + c*x + d;
	}

	public Interpolation(Iterable<Double> points) {
		List<Double> list = ImmutableList.copyOf(points);
		if (list.isEmpty()) {
			this.segments = ImmutableList.of(poly(0));
		} else {
			double constant = list.get(0);
			if (list.size() == 1) {
				this.segments = ImmutableList.of(poly(constant));
			} else if (list.size() == 2) {
				this.segments = ImmutableList.of(poly(list.get(1) - constant, constant));
			} else {
				double[] slope = new double[list.size() - 2];
				for (int i=0; i<slope.length; i++) {
					slope[i] = (list.get(i+2) - list.get(i)) / 2;
				}
				this.segments = new ArrayList<>(list.size() - 1);
				segments.add(firstSegment(list, slope));
				
				for (int i=1; i<slope.length; i++) {
					double s0 = slope[i-1];
					double s1 = slope[i];
					double y0 = list.get(i);
					double y1 = list.get(i+1);
					
					// a + b + s0 + y0 = y1
					// 3a + 2b + s0 = s1
					
					double a = s1 - 2*y1 + 2*y0 + s0;
					double b = y1 - y0 - s0 - a;
					
					Polynomial poly = poly(a, b, s0, y0);

					segments.add(poly);
				}
				
				segments.add(lastSegment(list, slope));
			}
		}
	}

	private Polynomial firstSegment(List<Double> points, double[] slope) {
		double constant = points.get(0);
		double s = slope[0];
		double y = points.get(1);
		// a + b + constant = y
		// 2a + b = s
		
		double a = s - y + constant;
		double b = y - constant - a;
		return poly(a, b, constant);
	}

	private Polynomial lastSegment(List<Double> points, double[] slope) {
		double s = slope[slope.length - 1];
		double constant = points.get(points.size() - 2);
		double y = points.get(points.size() - 1);
		
		// c + s + constant = y
		double c = y - s - constant;
		return poly(c, s, constant);
	}
	
	public double evaluate(double x) {
		if (x < 0) {
			return segments.get(0).evaluate(x);
		} else if (x >= segments.size()) {
			int index = segments.size() - 1;
			return segments.get(index).evaluate(x - index);
		} else {
			int index = (int)Math.floor(x);
			return segments.get(index).evaluate(x - index);
		}
	}
}
