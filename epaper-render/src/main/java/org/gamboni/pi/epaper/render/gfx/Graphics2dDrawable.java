/**
 * 
 */
package org.gamboni.pi.epaper.render.gfx;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;

import org.gamboni.pi.epaper.render.gfx.layout.Coords;
import org.gamboni.pi.epaper.render.widget.Widget;

import com.google.common.collect.ImmutableList;

/**
 * @author tendays
 *
 */
public abstract class Graphics2dDrawable implements Drawable {
	private final List<Graphics2D> targets;
	protected final int width;
	protected final int height;
	private final ImageLoader images;

	public Graphics2dDrawable(ImageLoader images, int width, int height, Graphics2D... targets) {
		this.images = images;
		this.targets = ImmutableList.copyOf(targets);
		this.width = width;
		this.height = height;
		
		foreachTarget(g -> {
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, width, height);
		});
	}
	
	@Override
	public void setOrigin(int x, int y, Runnable r) {
		foreachTarget(g -> g.translate(x, y));
		try {
			r.run();
		} finally {
			foreachTarget(g -> g.translate(-x, -y));
		}
	}

	@Override
	public void render(Widget wid, int left, int top, int right, int bottom) {
		setOrigin(left, top, () -> {
			foreachTarget(g -> {
				g.setClip(0, 0, right - left, bottom - top);
			});

			try {
				wid.render(this, right - left, bottom - top);
			} catch (Exception e) {
				e.printStackTrace();

				this.setColor(Color.RED);
				this.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
				this.drawString(e.toString(), 0, 10);
			}

		});
	}

	@Override
	public void drawString(String text, int x, int y) {
		foreachTarget(g -> g.drawString(text, x, y));
	}

	@Override
	public void fillRect(int x, int y, int w, int h) {
		foreachTarget(g -> g.fillRect(x, y, w, h));
	}
	
	@Override
	public Font getFont() {
		return targets.get(0).getFont();
	}

	@Override
	public Metrics getFontMetrics(Font font) {
		Graphics2D g = targets.get(0);
		return newMetrics(g.getFontMetrics(font));
	}

	@Override
	public void setFont(Font font) {
		foreachTarget(g -> g.setFont(font));
	}

	@Override
	public void setStroke(int width) {
		foreachTarget(g -> g.setStroke(new BasicStroke(width)));
	}

	@Override
	public void setColor(Color color) {
		foreachTarget(g -> g.setColor(color));
	}
	
	@Override
	public void drawPixel(int x, int y) {
		foreachTarget(g -> g.drawLine(x, y, x, y));
	}

	@Override
	public void drawLine(int x1, int y1, int x2, int y2) {
		foreachTarget(g -> g.drawLine(x1, y1, x2, y2));
	}
	
	@Override
	public void drawCircle(int x, int y, int r) {
		foreachTarget(g -> g.drawArc(x - r, y - r, r*2, r*2, 0, 360));
	}

	@Override
	public void drawCircle(Coords centre, int r) {
		foreachTarget(g -> g.drawArc((int)centre.getX() - r, (int)centre.getY() - r, r*2, r*2, 0, 360));
	}

	@Override
	public void fillCircle(Coords centre, int r) {
		foreachTarget(g -> g.fillOval((int)centre.getX() - r, (int)centre.getY() - r, r*2, r*2));
	}

	@Override
	public void drawLine(Coords from, Coords to) {
		drawLine((int)from.getX(), (int)from.getY(), (int)to.getX(), (int)to.getY());
	}

	@Override
	public void drawString(String string, Coords position) {
		drawString(string, (int)position.getX(), (int)position.getY());
	}

	@Override
	public void fillPolygon(List<Coords> points) {
		int[] x = new int[points.size()];
		int[] y = new int[points.size()];
		for (int i = 0; i<points.size(); i++) {
			x[i] = (int)points.get(i).getX();
			y[i] = (int)points.get(i).getY();
		}
		foreachTarget(g -> g.fillPolygon(x, y, points.size()));
	}

	
	@Override
	public void drawIcon(String name, Coords centre, int maxHeight, Anchor anchor, ImageStyle style) {
		BufferedImage icon = images.load(name);
		if (icon == null) {
			System.err.println("Could not find icon '"+ name +"'");
			return;
		}

		double scale = (icon.getHeight() > maxHeight) ? maxHeight / ((double)icon.getHeight()) : 1;

		int iconX = (int) (centre.getX() + anchor.getXOffset((int) (icon.getWidth() * scale)));
		int iconY = (int) (centre.getY() + anchor.getYOffset((int) (icon.getHeight() * scale)));
		setColor(Color.BLACK);
		
		/* 0: column fully transparent so far */
		/* 1: pixel right above is black */
		/* 2+: transparent pixels below black, to be made white if
		 * next is black. */
		int[] shadow = new int[icon.getWidth()];
		
		for (int y = 0; y < icon.getHeight()*scale; y++)
			for (int x = 0; x < icon.getWidth()*scale; x++) {
				/* "D"estination coordinates */
				int dx = x + iconX;
				int dy = y + iconY;
				int rgb = icon.getRGB((int) (x / scale), (int) (y / scale));
				if (style == ImageStyle.BW_EDGE) {
					// icon is black and white so testing one (blue) component is enough
					if ((rgb & 0xFF) < 128) {
						if (shadow[x] > 1) {
							setColor(Color.WHITE);
							drawLine(dx, dy - 1, dx, dy - shadow[x]);
							setColor(Color.BLACK);
						}
						drawPixel(dx, dy);
						shadow[x] = 1;
					} else if (shadow[x] != 0) {
						shadow[x]++;
					}
				} else {
					Color color = new Color(rgb);
					if (style == ImageStyle.LIGHT) {
						// extra /2 + 127 to make it brighter
						int gb = (color.getBlue() + color.getGreen()) / 4 + 127;
						int r = Math.max(color.getRed() / 2 + 127, gb);
						setColor(new Color(r, gb, gb));
					} else {
						setColor(color);
					}
					if (style != ImageStyle.WHITE_TRANSPARENT || !color.equals(Color.WHITE)) {
						drawPixel(dx, dy);
					}
				}
			}
	}

	@Override
	public Metrics getFontMetrics() {
		Graphics2D g = targets.get(0);
		FontMetrics fontMetrics = g.getFontMetrics();
		return newMetrics(fontMetrics);
	}

	protected Metrics newMetrics(FontMetrics fontMetrics) {
		return new Metrics(fontMetrics, targets.get(0), name -> {
			BufferedImage img = images.load(name);
			if (img == null) {
				return new Metrics.Size(0, 0);
			}
			return new Metrics.Size(img.getWidth()/2, img.getHeight()/2);
		});
	}

	protected void foreachTarget(Consumer<Graphics2D> action) {
		targets.forEach(action);
	}
	
	protected static BufferedImage newBufferedImage(int width, int height) {
		return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	}
}
