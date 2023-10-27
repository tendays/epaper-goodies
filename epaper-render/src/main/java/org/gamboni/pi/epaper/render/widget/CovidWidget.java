/**
 * 
 */
package org.gamboni.pi.epaper.render.widget;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.gamboni.pi.epaper.render.gfx.Drawable;
import org.gamboni.pi.epaper.render.gfx.layout.Coords;
import org.gamboni.pi.epaper.render.gfx.layout.RectCoords;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * @author tendays
 *
 */
public class CovidWidget implements Widget {

	static final int MARGIN_LEFT=30;
	static final int MARGIN_BOTTOM=11;
	static final int DAY_COUNT=35;
	static final File DATA_FILE = new File("covid");
	static final int DATE_TICK_INTERVAL=7;
	
	static class DataPoint {
		final LocalDate date;
		final int active;
		final int confirmed;
		DataPoint(LocalDate date, int value, int confirmed) {
			this.date = date;
			this.active = value;
			this.confirmed = confirmed;
		}
	}
	
	static class PlotArea {
		final int width, height;
		final LocalDate rightDate;
		final LocalDate leftDate;
		final BigDecimal topBigDecimal;
		final int bottomAxis;


		final int highest;
		public PlotArea(int width, int height, LocalDate rightDate, int highest) {
			this.width = width;
			this.height = height;
			this.highest = highest;
			this.topBigDecimal = BigDecimal.valueOf(highest);
			this.bottomAxis = height-MARGIN_BOTTOM;
			this.rightDate = rightDate;
			this.leftDate = rightDate.minusDays(DAY_COUNT);
		}
		public int convert(int value) {
			return convert(BigDecimal.valueOf(value));
		}
		public int convert(BigDecimal value) {
			return (int)(topBigDecimal.subtract(value).doubleValue() * bottomAxis / highest);
		}
		public int convert(LocalDate date) {
			long dayCount = date.toEpochDay() - leftDate.toEpochDay();
			return MARGIN_LEFT + pixelsForDays(dayCount);
		}
		public int pixelsForDays(long dayCount) {
			return (int)(dayCount * (width-MARGIN_LEFT) / DAY_COUNT);
		}
	}
	
	@Override
	public void render(Drawable graphics, int width, int height) {
		List<DataPoint> plot = new ArrayList<>();
		LocalDate rightDate = LocalDate.now();
		LocalDate leftDate = rightDate.minusDays(DAY_COUNT);
		try (LineNumberReader covid = new LineNumberReader(new FileReader(DATA_FILE))) {
			String line;
			while ((line = covid.readLine()) != null) {
				int space = line.indexOf(' ');
				int secondSpace = line.indexOf(' ', space + 1);
				DataPoint point = new DataPoint(
						LocalDate.parse(line.substring(0, space)),
						Integer.parseInt(line.substring(space+1, secondSpace)),
						Integer.parseInt(line.substring(secondSpace+1)));
				plot.add(point);
			}
		} catch (IOException io) {
			// ignore missing file
		}
		if (plot.isEmpty() || plot.get(plot.size() - 1).date.compareTo(LocalDate.now().minusDays(1)) < 0) {
			try (InputStream in = new URL("https://covid2019-api.herokuapp.com/country/Switzerland").openConnection().getInputStream()) {
				JsonObject json = new JsonParser().parse(new InputStreamReader(in)).getAsJsonObject();
				// {"Switzerland":{"confirmed":12928,"deaths":231,"recovered":1530},"dt":"3/27/20","ts":1585267200.0}
				JsonObject numbers = json.get("Switzerland").getAsJsonObject();
				int confirmed = numbers.get("confirmed").getAsInt();
				DataPoint dp = new DataPoint(
						LocalDate.parse(json.get("dt").getAsString(), DateTimeFormatter.ofPattern("M/d/yy")), // two-digit year
						confirmed - numbers.get("deaths").getAsInt() - numbers.get("recovered").getAsInt(), confirmed);
				if (dp.date.compareTo(plot.get(plot.size() - 1).date) > 0) {
					plot.add(dp);
				
					try (Writer out = new FileWriter(DATA_FILE, /*append*/true)) {
						out.write(dp.date.toString() +" "+ dp.active +" "+ dp.confirmed+"\n");
					}
				}
				
				
			} catch (IOException io) {
				io.printStackTrace();
			}
		}
		

		int highest = 1;
		for (int i = 1; i < plot.size(); i++) {
			int pushTop = (int)((plot.get(i).confirmed - plot.get(i-1).confirmed) * 1.05);
			if (!plot.get(i).date.isBefore(leftDate) && pushTop > highest) {
				highest = pushTop;
			}
		}

		PlotArea plotArea = new PlotArea(width, height, rightDate, highest);
		
		BigDecimal topTick = BigDecimal.valueOf(highest * 0.95);
		BigDecimal fraction = topTick.divide(new BigDecimal(5));
		// reduce precision to one digit
		BigDecimal tickStep = fraction.setScale(fraction.scale() - fraction.precision() + 1, RoundingMode.FLOOR);
		for (BigDecimal tick = BigDecimal.ZERO; tick.compareTo(topTick) <= 0; tick = tick.add(tickStep)) {
			int y = plotArea.convert(tick);
			if (tick.compareTo(BigDecimal.ZERO) > 0) {
				graphics.setColor(Color.gray);
				graphics.drawLine(MARGIN_LEFT - 1, y, width, y);
				graphics.setColor(Color.BLACK);
			}
			graphics.drawString(tick.toString(), 0, (int)(y + graphics.getFontMetrics().getAscent() / 2));
		}
		graphics.drawLine(MARGIN_LEFT, 0, MARGIN_LEFT, plotArea.bottomAxis);
		
		/* Horizontal axis */
		int modulo = 1;
		for (LocalDate tick = plotArea.leftDate.plusDays(1) ; !tick.isAfter(plotArea.rightDate) ; tick = tick.plusDays(1), modulo++) {
			graphics.setColor((modulo % DATE_TICK_INTERVAL == 0) ? Color.black : Color.gray);
			graphics.drawLine(
					plotArea.convert(tick), plotArea.bottomAxis,
					plotArea.convert(tick), 0);
		}
		/* Labels */
		graphics.setColor(Color.BLACK);
		for (LocalDate tick = plotArea.leftDate ; tick.isBefore(plotArea.rightDate) ; tick = tick.plusDays(DATE_TICK_INTERVAL)) {
			String label = tick.format(DateTimeFormatter.ofPattern("dMMM"));
			graphics.drawString(label,
					(int)(plotArea.convert(tick) -
					graphics.getFontMetrics().getStringBounds(label).getWidth() / 2),
					(int)(plotArea.bottomAxis + graphics.getFontMetrics().getAscent()));
		}
		graphics.drawLine(MARGIN_LEFT, plotArea.bottomAxis, width, plotArea.bottomAxis);
		
		
		/* Plot */
		Coords pen = null;
		int previousConfirmed = plot.isEmpty() ? 0 : plot.get(0).confirmed;
		for (DataPoint point : plot) {
			if (point.date.compareTo(plotArea.leftDate) >= 0) {
				int x = plotArea.convert(point.date);
				Coords next = new RectCoords(
						x,
						plotArea.convert(point.active));
				if (pen != null) {
/*					graphics.setColor(Color.BLACK);
					graphics.drawLine(pen, next);*/
					graphics.setColor(Color.RED);
					int barX = x - plotArea.pixelsForDays(1)/2;
					int barTop = plotArea.convert((point.confirmed - previousConfirmed));
					graphics.drawLine(barX, plotArea.convert(0), barX, barTop);
					graphics.drawLine(barX+1, plotArea.convert(0), barX+1, barTop);
				}
				
				pen = next;
			}
			previousConfirmed = point.confirmed;
		}
	}
}
