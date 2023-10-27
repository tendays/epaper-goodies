package org.gamboni.pi.epaper.render.gfx;

import com.google.common.io.ByteStreams;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ImageLoader {

    public static final File CACHE_DIR = new File("cache");
    final Map<String, BufferedImage> iconImages = new HashMap<>();

    public BufferedImage load(String name) {
        if (name.startsWith("http://") || name.startsWith("https://")) {
            return loadHttp(name);
        }
        try {
            InputStream stream = getClass().getClassLoader()
                    .getResourceAsStream(name + ".png");
            if (stream == null) {
                System.err.println("Could not open icon " + name);
                return null;
            }
            return ImageIO.read(stream);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private BufferedImage loadHttp(String name) {
        int doubleSlash = name.indexOf("://");
        File cacheName = new File(CACHE_DIR, name.substring(doubleSlash + 3).replaceAll("[/<>:&?]", "-"));
        try {
            return ImageIO.read(cacheName);
        } catch (IOException e) {
            // most likely: file doesn't exist, so first load to remote
            CACHE_DIR.mkdir();
            try (OutputStream output = new FileOutputStream(cacheName)) {
                HttpURLConnection connection = (HttpURLConnection) new URL(name).openConnection();
                ByteStreams.copy(connection.getInputStream(), output);
            } catch (IOException ex) {
                System.err.println("Could not load image at " + name);
                ex.printStackTrace();
                return null;
            }
            // file created, let's try loading it again:

            try {
                return ImageIO.read(cacheName);
            } catch (IOException ex) {
                System.err.println("Could not load image at " + name);
                ex.printStackTrace();
                return null;
            }
        }
    }
}
