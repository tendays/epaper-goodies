/**
 * 
 */
package org.gamboni.pi.epaper.render.widget;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.gamboni.pi.epaper.render.gfx.Drawable;

import com.google.common.io.CharStreams;

/** I use this to display my kids' pocket money, but it is actually just displaying the text returned by an HTTP GET
 * endpoint, so this can be used with any such endpoint.
 *
 * @author tendays
 */
public class AccountBalanceWidget implements Widget {
	
	private final URL url;

	public AccountBalanceWidget(String url) {
		try {
			this.url = new URL(url);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public void render(Drawable graphics, int width, int height) {
		graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));

		try {
			HttpURLConnection logonConnection = (HttpURLConnection) url
					.openConnection();

			try (InputStreamReader in = new InputStreamReader(logonConnection.getInputStream())) {
				graphics.setColor(Color.BLACK);
				graphics.drawString(CharStreams.toString(in), 0, 10);
			}
		} catch (IOException e) {
			graphics.setColor(Color.RED);
			graphics.drawString(e.toString(), 0, 10);
		}
	}

}
