/**
 * 
 */
package org.gamboni.pi.epaper.render.gfx.layout;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.gamboni.pi.epaper.render.gfx.Drawable;
import org.gamboni.pi.epaper.render.gfx.Graphics2dDrawable;
import org.gamboni.pi.epaper.render.gfx.ImageLoader;
import org.gamboni.pi.epaper.render.StackedMap;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * @author tendays
 *
 */
public class ElementCollection {
	private final List<LayoutElement> items = new ArrayList<>();
	private final Rectangle2D box;
	
	public ElementCollection(Rectangle2D box) {
		this.box = box;
	}

	public void add(LayoutElement elt) {
		items.add(elt);
	}
	
	private static class Assignment {
		final Map<LayoutElement, Map<AbstractPolarElement.LayoutParameter, Double>> values;
		double cost;
		public Assignment(Map<LayoutElement, Map<AbstractPolarElement.LayoutParameter, Double>> values, double cost) {
			this.values = values;
			this.cost = cost;
		}
	}
	
	private Map<LayoutElement, Map<AbstractPolarElement.LayoutParameter, Double>> layout(Drawable graphics) {
		Map<LayoutElement, Map<AbstractPolarElement.LayoutParameter, Double>> values = initialValues();
		Assignment best = null;
		int iteration = 0;
		
		System.out.println(this.items.size() +" elements");
		
		while (iteration < 200) {
			//debugRender(iteration, values);
			
			/* Compute surface of each element */
			Map<LayoutElement, Surface> surfaces = computeSurfaces(graphics, values);

			/* Detect collisions and apply forces */
			Map<LayoutElement, Force> forces = computeForces(surfaces);
			if (forces.isEmpty()) {
				System.out.println("No collision, returning early");
				return values;
			}

			double cost = computeCost(forces);
			
			boolean improvement = (best == null || best.cost > cost);
			if (improvement) {
				best = new Assignment(values, cost);
			}

			System.out.println("Cost = "+ cost +", iteration "+ iteration + (improvement ? " *" : ""));
			
			/* apply forces */
			values = applyForces(values, surfaces, forces, 0.01);
			
			iteration++;
		}
		
		/* Look for local minimum near best value */
		values = best.values;
		
		iteration = 0;
		boolean todo = true;
		while (todo) {
			todo = false;

			System.out.println("Local iteration #"+ iteration++);
			Map<LayoutElement, Map<AbstractPolarElement.LayoutParameter, Double>> overlay = new HashMap<>();
			Map<LayoutElement, Map<AbstractPolarElement.LayoutParameter, Double>> composite = new StackedMap<>(values, overlay);

			Map<LayoutElement, Surface> surfaces = computeSurfaces(graphics, values);
			Map<LayoutElement, Force> forces = computeForces(surfaces);
			for (LayoutElement item : items) {
				overlay.put(item, applyForces(item, composite.get(item), surfaces, forces, 0.34));
				Map<LayoutElement, Surface> compositeSurfaces = computeSurfaces(graphics, composite);
				double newCost = computeCost(computeForces(compositeSurfaces));
				if (newCost <= best.cost) {
					// replace best on equality (maybe going in right direction), but only set
					// 'todo' in case of actual improvement (avoiding risk of infinite loop)
					if (newCost < best.cost) {
						todo = true;
						System.out.println("Reduced cost to "+ newCost);
					}
					best = new Assignment(composite, newCost);
				} else {
					overlay.remove(item);
				}
			}
			values = ImmutableMap.copyOf(composite);
		}
		
		System.out.println("Force report at local minimum");
		computeForces(computeSurfaces(graphics, best.values)).forEach((item, f) -> {
			System.out.println(item +": "+ f);
		});
		
		return best.values;
	}

	private double computeCost(Map<LayoutElement, Force> forces) {
		double cost = 0;
		for (Force f : forces.values()) {
			cost += f.getIntensity();
		}
		return cost;
	}

	protected Map<LayoutElement, Surface> computeSurfaces(Drawable graphics,
			Map<LayoutElement, Map<AbstractPolarElement.LayoutParameter, Double>> values) {
		Map<LayoutElement, Surface> surfaces = new HashMap<>();
		for (LayoutElement item : items) {
			CoverageRecorder cr = new CoverageRecorder(graphics);
			item.render(cr, values.get(item));
			surfaces.put(item, cr.getCoverage());
		}
		return surfaces;
	}

 void debugRender(int iteration, Map<LayoutElement, Map<AbstractPolarElement.LayoutParameter, Double>> values) {
		int width = (int)box.getWidth();
		int height = (int)box.getHeight();
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2dDrawable out = new Graphics2dDrawable(new ImageLoader(), width, height, image.createGraphics()) {
			
			@Override
			public void debugBox(Coords topLeft, Coords bottomRight) {
				/*d.drawLine(topLeft.plus(width, 0), topLeft.plus(width, y));
				d.drawLine(topLeft.plus(0, y), topLeft.plus(width, y));*/
			}

			@Override
			public void save() throws IOException {
				ImageIO.write(image, "png", new File("iteration-" +
						Strings.padStart("" + iteration, 2, '0') + ".png"));
			}
		};
		out.setOrigin(width / 2, height / 2, () -> {
			for (LayoutElement item : items) {
				item.render(out, values.get(item), true);
			}
		});
		try {
			out.save();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Map<LayoutElement, Map<AbstractPolarElement.LayoutParameter, Double>> initialValues() {
		final Map<LayoutElement, Map<AbstractPolarElement.LayoutParameter, Double>> values = new HashMap<>();
		
		/* Initialise values to be as far as possible from each bound. */
		for (LayoutElement item : items) {
			values.put(item, new HashMap<>(Maps.transformValues(item.getBounds(),
					b -> (b.min + b.max) / 2)));
		}
		return values;
	}

	/** Apply the given force field to all layout elements.
	 * 
	 * @param values
	 * @param surfaces
	 * @param forces
	 * @param factor
	 * @return
	 */
	private ImmutableMap<LayoutElement, Map<AbstractPolarElement.LayoutParameter, Double>> applyForces(
            final Map<LayoutElement, Map<AbstractPolarElement.LayoutParameter, Double>> values, Map<LayoutElement, Surface> surfaces,
            Map<LayoutElement, Force> forces, double factor) {
		return ImmutableMap.copyOf(Maps.transformEntries(values,
				(item, params) -> applyForces(item, params, surfaces, forces, factor)));
	}

	/** Apply the given force field to a single layout element. */
	private @Nullable Map<AbstractPolarElement.LayoutParameter, Double> applyForces(@Nullable LayoutElement item,
                                                                                    @Nullable Map<AbstractPolarElement.LayoutParameter, Double> params, Map<LayoutElement, Surface> surfaces,
                                                                                    Map<LayoutElement, Force> forces, double factor) {
		Force force = forces.get(item);
		if (force == null) {
			return params;
		} else {
			Map<AbstractPolarElement.LayoutParameter, Force> slope = item.computeSlope(params, surfaces.get(item).getBoundingBox(), Corner.ofForce(force));
			return ImmutableMap.copyOf(
					Maps.transformEntries(params,
							(param, oldValue) -> {
								Force paramSlope = slope.get(param);
								double product = paramSlope.scalar(force);
								double newValue;
								if (Math.abs(product) > 0.0001) {
									double forceIntensity = force.getIntensity();
									newValue = oldValue + factor * forceIntensity*forceIntensity / product;
								} else {
									// slope orthogonal to force
									newValue = oldValue;
								}
								return bound(
										item.getBounds().get(param),
										newValue);
							}));
		}
	}

	private static double bound(AbstractPolarElement.Bounds bounds, double d) {
		if (d < bounds.min) {
			return bounds.min;
		} else if (d > bounds.max) {
			return bounds.max;
		} else {
			return d;
		}
	}

	private Map<LayoutElement, Force> computeForces(Map<LayoutElement, Surface> surfaces) {
		Map<LayoutElement, Force> forces = new HashMap<>();
		for (int i = 0; i<items.size(); i++) {
			LayoutElement item1 = items.get(i);
			Surface s1 = surfaces.get(item1);
			if (s1.isEmpty()) { continue; }

			/* let box boundaries exert forces on item1 */
			Force force1 = forces.getOrDefault(item1, Force.ZERO)
					.makeAtLeast(new Force(Direction.RIGHT, Math.max(box.getMinX() - s1.getL(), 0)))
					.makeAtLeast(new Force(Direction.DOWN, Math.max(box.getMinY() - s1.getT(), 0)))
					.makeAtLeast(new Force(Direction.LEFT, Math.max(s1.getR() - box.getMaxX(), 0)))
					.makeAtLeast(new Force(Direction.UP, Math.max(s1.getB() - box.getMaxY(), 0)));
			
			for (int j = i+1; j<items.size(); j++) {
				LayoutElement item2 = items.get(j);
				Surface s2 = surfaces.get(item2);
				if (s1.intersects(s2)) {
					// check in which direction we'd clear the intersection the quickest (suppose item1 is moving)
					Map.Entry<Direction, Double> components = ImmutableMap.of(
							Direction.LEFT, s1.getR() - s2.getL(),
							Direction.RIGHT, s1.getL() - s2.getR(),
							Direction.UP, s1.getB() - s2.getT(),
							Direction.DOWN, s1.getT() - s2.getB())
					.entrySet()
					.stream()
					.sorted(Comparator.comparing(Map.Entry::getValue))
					.findFirst() // shortest distance
					.get();
					
					Force force = new Force(components.getKey(), components.getValue());
					
					force1 = force1.makeAtLeast(force);
					forces.put(item2,
							forces.getOrDefault(item2, Force.ZERO).makeAtLeast(force.opposite()));
					
				}
			}
			if (force1.getIntensity() > 0) {
				forces.put(item1, force1);
			}
		}
		return forces;
	}
	
	public List<LayoutElement> getElements() {
		return ImmutableList.copyOf(this.items);
	}
	
	public void render(Drawable d) {
		Map<LayoutElement, Map<AbstractPolarElement.LayoutParameter, Double>> values = layout(d);
		for (LayoutElement item : items) {
			item.render(d, values.get(item));
		}
	}
}
