/**
 * 
 */
package org.gamboni.pi.epaper.render.widget;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.gamboni.pi.epaper.render.gfx.Drawable;
import org.gamboni.pi.epaper.render.gfx.layout.AbstractPolarElement;
import org.gamboni.pi.epaper.render.gfx.layout.ElementCollection;
import org.gamboni.pi.epaper.render.gfx.layout.PolarCoords;
import org.gamboni.pi.epaper.render.gfx.layout.TextElement;

/**
 * @author tendays
 *
 */
public class BigClockWidget extends WidgetHolder<ClockFace> implements Widget {
	
	public static class ClockCoordinates {
		final int radiusZero;
		final int radiusOne;
		private final ZonedDateTime now = ZonedDateTime.now();
		
		public ClockCoordinates(int radiusZero, int radiusOne) {
			this.radiusZero = radiusZero;
			this.radiusOne = radiusOne;
		}

		public boolean areHoursVisible(ZonedDateTime t) {
			return !t.isBefore(now) && !t.isAfter(now.plusHours(12));
		}
		
		public boolean areMinutesVisible(ZonedDateTime t) {
			return !t.isBefore(now) && !t.isAfter(now.plusHours(1));
		}
		
		public double convertDays(ZonedDateTime t) {
			/* Add 'local' components so that DST changes make "jumps". A day has the same length on the diagram, whatever the actual duration. */

			// day
			return PolarCoords.TWO_PI * (t.getDayOfWeek().getValue() - 1) / 7 +
					// hour
					PolarCoords.TWO_PI * t.getHour() / (7 * 24) +
					// minutes
					PolarCoords.TWO_PI * t.getMinute() / (7 * 24 * 60);
		}
		
		public double convertHours(ZonedDateTime t) {
			 // 2pi / 12 / 60
			return t.getHour() * Math.PI / 6.0
			 + t.getMinute() * Math.PI / 360.0;
		}

		public double convertMinutes(ZonedDateTime t) {
			return t.getMinute() * Math.PI / 30.0;
		}

		public double convertRadius(double ratio) {
			return radiusZero + (radiusOne - radiusZero) * ratio;
		}
	}
	
	@Override
	public void render(Drawable g, int width, int height) {
		List<Throwable> errors = new ArrayList<>();
		g.setOrigin(width / 2, height / 2, () -> {
			PolarCoords centre = PolarCoords.ORIGIN;
			
			final ElementCollection elements = new ElementCollection(new Rectangle2D.Double(
					-width/2, -height/2,
					width, height));
			
			g.setColor(Color.BLACK);
			int visibleRadius = Math.min(width, height) / 2;
			int faceRadius = visibleRadius - 55;
			int crownRadius = visibleRadius + 20; // overflow a bit
			g.drawCircle(0, 0, faceRadius);
			ClockCoordinates faceCoords = new ClockCoordinates(0, faceRadius);
			ClockCoordinates crownCoords = new ClockCoordinates(faceRadius, crownRadius);

			try {
				renderWidgets(r -> r.renderRadial(g, faceCoords, elements));
			} catch (Exception x) {
				errors.add(x);
			}

			g.setColor(Color.BLACK);
			ZonedDateTime t = ZonedDateTime.now();
			/* Hour clock hand */
			g.drawLine(centre, new PolarCoords(faceCoords.convertRadius(0.5), faceCoords.convertHours(t)));

			/* Minute clock hand */
			g.drawLine(centre, new PolarCoords(faceCoords.convertRadius(1), faceCoords.convertMinutes(t)));

			g.setColor(Color.BLACK);
			//g.drawCircle(0, 0, crownRadius);
			for (int delta = 0; delta<7; delta++) {
				LocalDate day = LocalDate.now().plusDays(delta);
				ZonedDateTime dayStart = day.atStartOfDay(ZoneId.systemDefault());

				g.drawLine(new PolarCoords(crownCoords.convertRadius(0), crownCoords.convertDays(dayStart)),
						new PolarCoords(crownCoords.convertRadius(1), crownCoords.convertDays(dayStart)));

				// add two hours so the number fits nicely within the day instead of overlapping with previous day
				double datePoint = crownCoords.convertDays(dayStart.plusHours(2).plusMinutes(20));
				elements.add(new TextElement(
						String.valueOf(day.getDayOfMonth()),
						TextElement.Placement.CENTRED,
						new Font(Font.SERIF, Font.PLAIN, 20),
						(delta == 0) ? Color.RED : Color.BLACK,
								AbstractPolarElement.Parameters.radius(crownCoords.convertRadius(0.5))
								.direction(datePoint)
								.width(20) // doesn't actually matter, numbers can't be split
								.anchor(0, 0)
								));

				try {
					renderWidgets(face -> {
						if (face instanceof CrownFace) {
							((CrownFace) face).renderCrown(g, crownCoords, day, elements);
						}
					});
				} catch (Exception x) {
					x.printStackTrace();
					errors.add(x);
				}
			}
			
			try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("elements-dump"))) {
				out.writeObject(elements.getElements());
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			elements.render(g);
		});
		
		if (!errors.isEmpty()) {
			throw new RuntimeException(errors.stream().map(Throwable::toString).collect(Collectors.joining("; ")));
		}
	}
}
