/**
 * 
 */
package org.gamboni.pi.epaper.render.gfx;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.gamboni.pi.epaper.render.constraint.Assignment;
import org.gamboni.pi.epaper.render.constraint.Constraint;
import org.gamboni.pi.epaper.render.constraint.ConstraintSolver;
import org.gamboni.pi.epaper.render.constraint.Variable;

import com.google.common.collect.Iterables;

/**
 * @author tendays
 *
 */
public class DynamicFixedWidthWriter extends FixedWidthWriter {
	
	private abstract class Item {
		public final Variable y = solver.addVariable();
		protected abstract int getHeight();
		protected abstract void render(Assignment solvedConstraints);
	}

	private class TextLine extends Item {
		final String text;
		final int indent;
		final Color color;
		TextLine(String text, int indent, Color color) {
			this.text = text;
			this.indent = indent;
			this.color = color;
		}
		@Override
		protected int getHeight() {
			return lineHeight;
		}
		@Override
		protected void render(Assignment solvedConstraints) {
			graphics.setColor(color);
			graphics.drawString(text, left + indent, top + solvedConstraints.get(y) + lineHeight);
		}
	}
	
	public class Spacer extends Item {
		
		final int height;

		public Spacer(int height) {
			this.height = height;
		}

		@Override
		protected int getHeight() {
			return height;
		}

		@Override
		protected void render(Assignment solvedConstraints) {
		}
	}
	
	private class Rectangle {
		final int indent, right;
		final Variable topVar;
		final Variable bottomVar;
		final int borderWidth;
		final int padding;
		
		public Rectangle(int indent, Variable topVar, int right, Variable bottomVar, int borderWidth, int padding) {
			this.indent = indent;
			this.right = right;
			this.topVar = topVar;
			this.bottomVar = bottomVar;
			this.borderWidth = borderWidth;
			this.padding = padding;
		}

		public void render(Assignment assignment) {
			drawRectangle(borderWidth, left + indent, top + assignment.get(topVar), left + right, top + assignment.get(bottomVar) + borderWidth + padding);
		}
	}
	
	private final ConstraintSolver solver;
	private final List<Item> stuff = new ArrayList<>();
	private final List<Item> obstacles = new ArrayList<>();
	private final List<Rectangle> rectangles = new ArrayList<>();

	public DynamicFixedWidthWriter(ConstraintSolver solver, Drawable graphics, int left, int top, int width) {
		super(graphics, left, top, width);
		this.solver = solver;
	}

	@Override
	protected void printLine(String string, int x, Color color) {
		add(new TextLine(string, x, color));
	}

	@Override
	protected Border startBorder(int border, int padding) {
		Spacer topSpacer = addSpacer(border + padding);
		return width -> {
			Spacer bottomSpacer = addSpacer(border + padding);
			rectangles.add(new Rectangle(0, topSpacer.y, width, bottomSpacer.y, border, padding));
		};
	}
	
	public Spacer getLatestSpacer() {
		return (Spacer) Iterables.getLast(stuff);
	}

	private void add(Item item) {
		if (!stuff.isEmpty()) {
			Item last = stuff.get(stuff.size() - 1);
			solver.addConstraint(new Constraint.AtLeastVar(last.y, item.y, last.getHeight()));
		}
		stuff.add(item);
		
		for (Item obstacle : obstacles) {
			noCollision(item, obstacle);
		}
	}
	
	public void avoid(Spacer spacer) {
		for (Item other : Iterables.concat(stuff, obstacles)) {
			noCollision(spacer, other);
		}
		obstacles.add(spacer);
	}

	private void noCollision(Item one, Item other) {
		solver.addConstraint(new Constraint.NoCollision(one.y, other.y, -one.getHeight(), other.getHeight()));
	}

	public Spacer addSpacer(int height) {
		Spacer spacer = new Spacer(height);
		add(spacer);
		return spacer;
	}
	
	public void render(Assignment solvedConstraints) {
		graphics.setColor(Color.GRAY);
		for (Rectangle rect : rectangles) {
			rect.render(solvedConstraints);
		}

		graphics.setColor(Color.BLACK);
		for (Item item : stuff) {
			item.render(solvedConstraints);
		}
	}
}
