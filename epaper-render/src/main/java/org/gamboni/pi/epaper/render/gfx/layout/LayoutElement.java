/**
 * 
 */
package org.gamboni.pi.epaper.render.gfx.layout;

import java.awt.geom.Rectangle2D;
import java.util.Map;

import org.gamboni.pi.epaper.render.gfx.Drawable;

import com.google.common.collect.ImmutableMap;

/**
 * @author tendays
 *
 */
public interface LayoutElement {

	default void render(Drawable d, Map<AbstractPolarElement.LayoutParameter, Double> values) {
		render(d, values, false);
	}
	
	void render(Drawable d, Map<AbstractPolarElement.LayoutParameter, Double> values, boolean debug);

	ImmutableMap<AbstractPolarElement.LayoutParameter, AbstractPolarElement.Bounds> getBounds();

	/** Estimate, for each parameter, the position variation of the given corner when the parameter is changed by an epsilon.
	 * 
	 * @param params the current parameters
	 * @param boundingBox the corresponding bounding box of this element
	 * @param corner the corner whose position is to compute
	 * @return a Map associating to each parameter the corresponding corner direction (reusing the {@link Force} class to express a vector)
	 */
	Map<AbstractPolarElement.LayoutParameter, Force> computeSlope(Map<AbstractPolarElement.LayoutParameter, Double> params, Rectangle2D boundingBox, Corner corner);

}