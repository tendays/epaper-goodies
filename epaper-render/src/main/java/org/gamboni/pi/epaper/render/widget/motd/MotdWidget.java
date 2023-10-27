/**
 * 
 */
package org.gamboni.pi.epaper.render.widget.motd;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.gamboni.pi.epaper.render.gfx.Drawable;
import org.gamboni.pi.epaper.render.gfx.FixedWidthWriter;
import org.gamboni.pi.epaper.render.gfx.StaticFixedWidthWriter;
import org.gamboni.pi.epaper.render.widget.Widget;

import com.google.common.collect.ImmutableList;

/**
 * @author tendays
 *
 */
public class MotdWidget implements Widget {
	
	public interface Face {
		Optional<Message> getMotd();
	}
	
	public enum Priority {
		BACKGROUND(Color.BLACK), // only show if there's nothing interesting to say
		DEFAULT(Color.BLACK), // normal message
		IMPORTANT(Color.RED); // urgent one, shown in red
		
		public final Color color;
		private Priority(Color color) {
			this.color = color;
		}
	}
	
	public static class Message {
		public final String text;
		public final Priority priority;
		public Message(String text, Priority priority) {
			this.text = text;
			this.priority = priority;
		}
		public static Optional<Message> of(String text, Priority priority) {
			return Optional.of(new Message(text, priority));
		}
		
		public String toString() {
			return priority +": "+ text;
		}
	}
	
	private final ImmutableList<Face> faces;

	public MotdWidget(List<Face> faces) {
		this.faces = ImmutableList.copyOf(faces);
	}

	@Override
	public void render(Drawable graphics, int width, int height) {
		List<Message> messages = new ArrayList<>();
		for (Face face : faces) {
			face.getMotd().ifPresent(messages::add);
		}
		graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
		FixedWidthWriter w = new StaticFixedWidthWriter(graphics, 2, (int)graphics.getFontMetrics().getAscent() + 2, width - 2);
		for (Message message : messages) {
			w.print(message.text, message.priority.color);
			w.newLine();
		}
	}

}
