/**
 * 
 */
package org.gamboni.pi.epaper.render.widget;

import java.awt.Color;
import java.awt.Font;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.gamboni.pi.epaper.render.gfx.Drawable;

/**
 * @author tendays
 *
 */
public class DateTimeWidget implements Widget {

	@Override
	public void render(Drawable graphics, int width, int height) {
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, width, height);
		
		graphics.setColor(Color.BLACK);
		graphics.setFont(new Font(Font.SERIF, Font.BOLD, height / 2));
		graphics.drawString(DateTimeFormatter.ofPattern("d MMMM / HH:mm").format(LocalDateTime.now()),
				0, height - 4);
	}
}
