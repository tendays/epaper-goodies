/**
 * 
 */
package org.gamboni.pi.epaper.render.widget;

import java.awt.Color;
import java.awt.Font;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.gamboni.pi.epaper.render.Format;
import org.gamboni.pi.epaper.render.gfx.Drawable;
import org.gamboni.pi.epaper.render.gfx.DynamicFixedWidthWriter;
import org.gamboni.pi.epaper.render.gfx.layout.RectCoords;
import org.gamboni.pi.epaper.render.calendar.EventInfo;
import org.gamboni.pi.epaper.render.constraint.Assignment;
import org.gamboni.pi.epaper.render.constraint.ConstraintSolver;
import org.gamboni.pi.epaper.render.constraint.Variable;

/**
 * @author tendays
 *
 */
public class MonthCalendarLayout implements CalendarLayout {
	private LocalDate topLeft;
	private LocalDate bottomRight;
	private int lineCount;
	
	private static class Range {
		public final LocalDate topLeft;
		public final LocalDate bottomRight;
		public Range(LocalDate dayOne, LocalDate lastDay) {
			this.topLeft = dayOne.minusDays(dayOne.getDayOfWeek().getValue() - 1);
			this.bottomRight = lastDay.plusDays(7 - lastDay.getDayOfWeek().getValue()); 
		}
	}
	
	public MonthCalendarLayout(Range range) {
		this.topLeft = range.topLeft;
		this.bottomRight = range.bottomRight;
		
		this.lineCount = (int)ChronoUnit.DAYS.between(topLeft, bottomRight.plusDays(1)) / 7;
	}
	
	public static Range currentMonth() {
		LocalDate today = LocalDate.now();
		LocalDate dayOne = today.withDayOfMonth(1);
		return new Range(today.withDayOfMonth(1), dayOne.plusMonths(1).minusDays(1));
	}
	
	public static Range nextWeeks(int count) {
		LocalDate today = LocalDate.now();
		return new Range(today, today.plusWeeks(count));
	}
	
	private class Page {
		private final int width;
		private final int height;
		
		public Page(int width, int height) {
			this.width = width;
			this.height = height;
		}

		public int getLineNumber(LocalDate day) {
			 return (int)ChronoUnit.DAYS.between(topLeft, day) / 7;
		}

		public int getLeft(LocalDate day) {
			return width * (day.getDayOfWeek().getValue() - 1) / 7;
		}

		public int getRight(LocalDate day) {
			return width * (day.getDayOfWeek().getValue()) / 7;
		}

		public int getTop(int y) {
			return height * y / lineCount;
		}

		public int getTop(LocalDate day) {
			return getTop(getLineNumber(day));
		}

		public int getBottom(LocalDate day) {
			return getTop(getLineNumber(day) + 1);
		}
	}
	
	@Override
	public void render(Drawable g, int width, int height, List<EventInfo> data) {
		ConstraintSolver solver = new ConstraintSolver();
		Page page = new Page(width, height);
		g.setColor(Color.BLACK);
		for (int x = 1; x < 7; x++) {
			g.drawLine(width * x / 7, 0, width * x / 7, height);
		}
		for (int y = 1; y < lineCount; y++) {
			g.drawLine(0, page.getTop(y), width, height * y / lineCount);
		}
		g.setFont(new Font(Font.DIALOG, Font.PLAIN, height / lineCount / 2));
		LocalDate today = LocalDate.now();
		for (LocalDate day = topLeft; !day.isAfter(bottomRight); day = day.plusDays(1)) {
			int lineNumber = page.getLineNumber(day);
			
			if (day.equals(today)) {
				g.setColor(Color.LIGHT_GRAY);
				
				int left = page.getLeft(day)+1;
				int top = page.getTop(day);
				g.fillRect(left, top,
						page.getRight(day) - left, page.getBottom(day) - top);
				g.setColor(Color.WHITE);
			} else {
				g.setColor(Color.LIGHT_GRAY);
			}
			g.drawString(Integer.toString(day.getDayOfMonth()),
					page.getLeft(day),
					(int)(height * (lineNumber + 0.75) / lineCount));
			if (day.isBefore(today)) {
				g.setColor(Color.DARK_GRAY);
				g.drawLine(page.getLeft(day), page.getBottom(day), page.getRight(day), page.getTop(day));
			}
		}
		g.setFont(new Font(Font.SERIF, Font.PLAIN, 20));
		g.setColor(Color.BLACK);
		Map<LocalDate, DynamicFixedWidthWriter> writers = new TreeMap<>();
		List<Line> lines = new ArrayList<>();
		Line currentLine = null;
		for (EventInfo event : data) {
			LocalDate startDate = event.from.toLocalDate();
			DynamicFixedWidthWriter writer = getDayWriter(g, width, solver, page, writers, startDate);
			String displayedText = (event.hasTime ? Format.TIME_PATTERN.format(event.from) + " " : "") +
					event.text;
			
			// "exclusive end date". So 00:00 on Tuesday turns into 23:59 on Monday, and toLocalDate() is Monday instead of Tuesday
			LocalDate endDate = event.to.withZoneSameInstant(ZoneId.of("Z")).minusMinutes(1).toLocalDate();
			if (endDate.equals(startDate)) {
				writer.print(displayedText, event.color);
			} else {
				writer.printWithBorder(displayedText, 1, 1, event.color);
				DynamicFixedWidthWriter.Spacer spacer = writer.getLatestSpacer();
				currentLine = lineForDay(page, startDate, spacer, true);
				lines.add(currentLine);
				LocalDate currentDate = startDate.plusDays(1);
				while (!currentDate.isAfter(endDate)) {
					int currentTop = page.getTop(currentDate);
					if (currentTop == currentLine.top) {
						currentLine.extend(page.getRight(currentDate) - 2);
					} else {
						currentLine = lineForDay(page, currentDate, spacer, false);
						lines.add(currentLine);
					}
					getDayWriter(g, width, solver, page, writers, currentDate).avoid(spacer);
					currentDate = currentDate.plusDays(1);
				}
			}
			if (event.image != null) {
				int lineNumber = page.getLineNumber(startDate);
				g.drawIcon(event.image.toString(),
						new RectCoords(page.getRight(startDate), (page.getTop(startDate) + page.getBottom(startDate))/2),
						page.getTop(lineNumber + 1) - page.getTop(lineNumber),
						Drawable.Anchor.RIGHT,
						Drawable.ImageStyle.LIGHT);
			}
		}
		Assignment assignment = solver.solve();
		g.setColor(Color.BLACK);
		for (Line line : lines) {
			line.render(g, assignment);
		}
		g.setColor(Color.BLACK);
		for (DynamicFixedWidthWriter writer : writers.values()) {
			writer.render(assignment);
		}
	}

	protected Line lineForDay(Page page, LocalDate day, DynamicFixedWidthWriter.Spacer spacer, boolean start) {
		return new Line(spacer.y, page.getLeft(day) + (start ? 2 : 0), page.getTop(day), page.getRight(day) - 2);
	}
	
	private static class Line {
		
		private final Variable varY;
		private final int left;
		private final int top;
		private int right; // not final because extend()

		public Line(Variable varY, int left, int top, int right) {
			this.varY = varY;
			this.left = left;
			this.top = top;
			this.right = right;
		}
		
		public void extend(int newRight) {
			this.right = newRight;
		}

		public void render(Drawable g, Assignment assignment) {
			int y = assignment.get(varY) + top + 4;
			g.drawLine(left, y, right, y);
		}
	}

	private DynamicFixedWidthWriter getDayWriter(Drawable g, int width, ConstraintSolver solver, Page page,
			Map<LocalDate, DynamicFixedWidthWriter> writers, LocalDate day) {
		return writers.computeIfAbsent(day, __ -> {
			DynamicFixedWidthWriter newWriter = new DynamicFixedWidthWriter(solver, g, page.getLeft(day) + 2, page.getTop(day) + 2, width / 7 - 4);
			return newWriter;
		});
	}

	@Override
	public LocalDate getMinDate() {
		return topLeft;
	}

	@Override
	public LocalDate getMaxDate() {
		return bottomRight;
	}
}
