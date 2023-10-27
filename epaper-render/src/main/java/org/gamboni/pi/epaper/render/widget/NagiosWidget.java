/**
 * 
 */
package org.gamboni.pi.epaper.render.widget;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.imageio.ImageIO;

import org.gamboni.pi.epaper.render.gfx.Drawable;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * @author tendays
 *
 */
public class NagiosWidget implements Widget {
	
	private final String url;

	public NagiosWidget(String cgiUrl, String host, String service, List<String> db, String rrdopts) {
			this.url = cgiUrl +"?"+ param("host", host) +
			 "&"+ param("service", service) +
			 Joiner.on("").join(Lists.transform(db, v -> "&"+ param("db", v))) +
			 "&"+ param("rrdopts", rrdopts);
			// http://jeera.gamboni.org/nagiosgraph/cgi-bin/showgraph.cgi?host=localhost&service=Qmail%20traffic&db=clean&db=spam&db=out&geom=256x120&
			// rrdopts=%20-g%20-snow-777600%20-enow-0%20
	}

	private String param(String key, String value) {
		try {
			return key +"="+ URLEncoder.encode(value, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void render(Drawable graphics, int width, int height) {
		try {
			URL nagiosUrl = new URL(url +"&"+ param("geom", (width-5) +"x"+ (height - 20)));
			int offx = 28;
			int offy = 7;
			
			BufferedImage graphImage = ImageIO.read(nagiosUrl);
			if (graphImage.getWidth() - offx < width) {
				width = graphImage.getWidth() - offx;
			}
			if (graphImage.getHeight() - offy < height) {
				height = graphImage.getHeight() - offy;
			}
			for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
				int rgb = graphImage.getRGB(x + offx, y + offy);
				int r = (rgb & 0xFF0000) >> 16;
				int g = (rgb & 0x00FF00) >> 8;
				int b = (rgb & 0x0000FF);
				if (r < 65 && g < 65 && b < 65) {
					// black: labels
					graphics.setColor(Color.BLACK);
					graphics.drawPixel(x, y);
				} else if (r == g && g == b) {
					// grey: background
				} else if (b > g && b > r) {
					// blue: important, convert to red
					graphics.setColor(Color.RED);
					graphics.drawPixel(x, y);
				} else {
					// green: normal: black
					graphics.setColor(Color.BLACK);
					graphics.drawPixel(x, y);
				}
			}
		} catch (IOException e) {
			graphics.setColor(Color.RED);
			graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
			graphics.drawString(e.getMessage(), 0, 10);
		}
	}
}
