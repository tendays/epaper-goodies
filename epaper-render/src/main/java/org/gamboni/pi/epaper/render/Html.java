/**
 * 
 */
package org.gamboni.pi.epaper.render;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/**
 * @author tendays
 *
 */
public abstract class Html {

	public static String extractText(String html) {
		StringBuilder text = new StringBuilder();
		dump(text, Jsoup.parse(html));
		return text.toString().trim();
	}

	private static void dump(StringBuilder builder, Node n) {
		if (n instanceof TextNode) {
			builder.append(((TextNode)n).getWholeText());
		}
		n.childNodes().forEach(child -> dump(builder, child));
	}
}
