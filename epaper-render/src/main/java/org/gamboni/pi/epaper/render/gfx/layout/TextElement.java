/**
 * 
 */
package org.gamboni.pi.epaper.render.gfx.layout;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.ObjDoubleConsumer;

import org.gamboni.pi.epaper.render.gfx.Drawable;
import org.gamboni.pi.epaper.render.gfx.Metrics;

import com.google.common.base.CharMatcher;

/**
 * @author tendays
 *
 */
public class TextElement extends AbstractPolarElement {
	private static final long serialVersionUID = 4720538100392972213L;
	private static final CharMatcher DELIMITER = CharMatcher.anyOf(" ,-;\n");
	
	private static class Word {
		final String w;
		final Coords coords;
		
		Word(String w, double x, double y) {
			this.w = w;
			this.coords = new RectCoords(x, y);
		}
	}
	
	public enum Placement {
		/** Display the text only, aligning its centre at the calculated position. */
		CENTRED {

			@Override
			public Coords getOrigin(Coords coords, double w, double h, double anchor) {
				return coords.plus(-w/2, -h/2);
			}
		},

		/** Place an empty circle at the calculated position and place the text along with it. */
		EMPTY_MARKER {

			@Override
			public Coords getOrigin(Coords coords, double w, double h, double anchor) {
				return coords.plus(getSideOffset(w, h, anchor));
			}

			@Override
			public void render(Drawable d, Coords coords, double w, double h, double anchor, List<Word> words) {
				d.drawCircle(coords, 3);
				super.render(d, coords, w, h, anchor, words);
			}
		},
		/** Place a filled circle at the calculated position and place the text along with it. */
		FILLED_MARKER {

			@Override
			public Coords getOrigin(Coords coords, double w, double h, double anchor) {
				return coords.plus(getSideOffset(w, h, anchor));
			}

			@Override
			public void render(Drawable d, Coords coords, double w, double h, double anchor, List<Word> words) {
				d.fillCircle(coords, 3);
				super.render(d, coords, w, h, anchor, words);
			}
		},
		
		/** Arrow pointing outwards at the anchor */
		ARROW {
			@Override
			public Coords getOrigin(Coords coords, double w, double h, double anchor) {
				// todo: adjust, these are tailored for a circle
				return coords.plus(getSideOffset(w, h, anchor));
			}

			@Override
			public void render(Drawable d, Coords coords, double w, double h, double anchor, List<Word> words) {
				coords.getRadius();
				coords.getAngle();
				// TODO d.drawLine(coords, to);
				super.render(d, coords, w, h, anchor, words);
			}
			
		};
		
		protected Coords getSideOffset(double w, double h, double anchor) {
			/* largest distance from origin */
			double paddedW = w+4;
			double paddedH = h+4;
			/* travel distance as offset moves from 0 to 1 */
			double fullW = w+8;
			double fullH = h+8;
			Coords[] delta = new Coords[1]; // mutable final hack
			splitAnchor(anchor, (side, offset) -> {
				switch (side) {
				case BOTTOM:
					delta[0] = new RectCoords(fullW*offset - paddedW, -paddedH);
					break;
				case LEFT:
					delta[0] = new RectCoords(4, fullH*offset - paddedH);
					break;
				case TOP:
					delta[0] = new RectCoords(4 - fullW*offset, 4);
					break;
				case RIGHT:
					delta[0] = new RectCoords(-paddedW, 4 - fullH*offset);
					break;
				}
			});
			return delta[0];
		}

		public abstract Coords getOrigin(Coords coords, double w, double h, double anchor);
		
		public void render(Drawable d, Coords coords, double w, double h, double anchor, List<Word> words) {
			for (Word word : words) {
				d.drawString(word.w, word.coords.plus(getOrigin(coords, w, h, anchor)));
			}
		}
	}
	
	public final String text;
	public final Placement placement;
	public final Font font;
	public final Color color;
	public TextElement(String text, Placement placement, Font font, Color color, Parameters parameters) {
		super(parameters.bounds);
		this.text = text;
		this.placement = placement;
		this.font = font;
		this.color = color;
	}
	
	private enum Side {
		BOTTOM, LEFT, TOP, RIGHT;
	}
	
	private static void splitAnchor(double a, ObjDoubleConsumer<Side> callback) {
		double mod = a % 4;
		if (mod < 0) {
			mod += 4;
		}
		
		if (mod < 1) {
			callback.accept(Side.BOTTOM, mod);
		} else if (mod < 2) {
			callback.accept(Side.LEFT, mod - 1);
		} else if (mod < 3) {
			callback.accept(Side.TOP, mod - 2);
		} else {
			callback.accept(Side.RIGHT, mod - 3);
		}
	}
	
	@Override
	public Map<LayoutParameter, Force> computeSlope(Map<LayoutParameter, Double> params, Rectangle2D boundingBox,
			Corner corner) {
		Map<LayoutParameter, Force> result = super.computeSlope(params, boundingBox, corner);
		
		double width = params.get(LayoutParameter.WIDTH);
		double surface = boundingBox.getWidth() * boundingBox.getHeight();
		// height is estimated to surface / width, whose derivative according to width is  - surface / sq(width)
		double deltaHeight = - surface / (width * width);
		
		result.put(LayoutParameter.WIDTH, computeWidthSlope(corner, deltaHeight));
		
		if (placement == Placement.CENTRED) {
			result.put(LayoutParameter.ANCHOR, Force.ZERO);
		} else {
			splitAnchor(params.get(LayoutParameter.ANCHOR), (side, offset) -> {
				switch (side) {
				case BOTTOM:
					result.put(LayoutParameter.ANCHOR, new Force(boundingBox.getWidth() + 8, 0));
					break;
				case LEFT:
					result.put(LayoutParameter.ANCHOR, new Force(0, boundingBox.getHeight() + 8));
					break;
				case TOP:
					result.put(LayoutParameter.ANCHOR, new Force(-boundingBox.getWidth() - 8, 0));
					break;
				case RIGHT:
					result.put(LayoutParameter.ANCHOR, new Force(0, -boundingBox.getHeight() - 8));
					break;
				}
			});
		}		
		return result;
	}

	private Force computeWidthSlope(Corner corner, double deltaHeight) {
		if (placement == Placement.CENTRED) {
			return new Force(corner.x / 2.0, corner.y * deltaHeight / 2);
		} else {
			// (assume text is anchored at top-left for now)
			switch (corner) {
			case BOTTOM_LEFT:
				return new Force(0, deltaHeight);
			case BOTTOM_RIGHT:
				return new Force(1, deltaHeight);
			case TOP_LEFT:
				return Force.ZERO;
			case TOP_RIGHT:
				return new Force(1, 0);
			}
			throw new IllegalArgumentException(corner.name());
		}
	}

	@Override
	public void render(Drawable d, Map<LayoutParameter, Double> values, boolean debug) {
		PolarCoords anchorCoords = getAnchorCoords(values);
		int width = values.get(LayoutParameter.WIDTH).intValue();
		
		d.setFont(font);
		d.setColor(color);
		Metrics metrics = d.getFontMetrics();

		/* Layout words in this list first, as we don't yet know the bounding box. */
		List<Word> words = new ArrayList<>();
		// bounding box width
		double bbw = 0;
		int lineSpacing = (int)metrics.getAscent() - 1;
		
		// current word boundaries
		int start = 0;
		int end;
		int x = 0;
		int y = (int)metrics.getAscent();
		while (start < text.length()) {
			// take delimiter characters one by one
			end = DELIMITER.matches(text.charAt(start)) ? start + 1 : DELIMITER.indexIn(text, start + 1);
			if (end == -1) {
				end = text.length();
			}
			String word = text.substring(start, end);
			Rectangle2D wordBounds = metrics.getStringBounds(word);
			if (!word.trim().isEmpty()) {
				// check if the word fits in the current line
				// NOTE! if word is blank we allow going beyond the right margin
				double wordRightEdge = x + wordBounds.getWidth();
				if (x > 0 && wordRightEdge > width) {
					x = 0;
					y += lineSpacing;
				} else if (wordRightEdge > bbw) { 
					bbw = wordRightEdge;
				}
				words.add(new Word(word, x, y));
			} else if (word.equals("\n")) {
				x = 0;
				y += lineSpacing;
			}
			x += wordBounds.getWidth();
			start = end;
		}
		
		Double anchor = values.get(LayoutParameter.ANCHOR);
		this.placement.render(d, anchorCoords, bbw, y, anchor, words);
		if (debug) {
			Coords topLeft = placement.getOrigin(anchorCoords, bbw, y, anchor);
			d.debugBox(topLeft, topLeft.plus(width, y));
		}
	}
	
	public String toString() {
		return getClass().getSimpleName() +"["+ text +"]";
	}
}
