/**
 * 
 */
package org.gamboni.pi.epaper.render.gfx.layout;

import java.util.Map;

import org.gamboni.pi.epaper.render.gfx.Drawable;

/**
 * @author tendays
 *
 */
public class IconElement extends AbstractPolarElement {
	private static final long serialVersionUID = -4960256420745024441L;
	
	private final String name;
	private final Drawable.ImageStyle style;
	private final int maxHeight;

	public IconElement(String name, Wa centre, int maxHeight, Drawable.ImageStyle style) {
		super(centre.getBounds());
		this.name = name;
		this.maxHeight = maxHeight;
		this.style = style;
	}

	@Override
	public void render(Drawable d, Map<LayoutParameter, Double> values, boolean debug) {
		d.drawIcon(name, getAnchorCoords(values), maxHeight, Drawable.Anchor.CENTRE, style);
	}
	
	public String toString() {
		return getClass().getSimpleName() +"["+ name +"]";
	}
}
