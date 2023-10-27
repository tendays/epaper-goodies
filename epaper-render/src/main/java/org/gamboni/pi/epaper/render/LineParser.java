package org.gamboni.pi.epaper.render;

import java.util.List;

import com.google.common.base.Splitter;

public class LineParser {
	String args;
	LineParser(String args) {
		this.args = args.replace("\\n", "\n");
	}

	String getString() {
		args = args.trim();
		if (args.startsWith("\"")) {
			int closing = args.indexOf("\"", 1);
			if (closing == -1) {
				throw new IllegalArgumentException(args +": unmatched \"");
			}
			return result(args.substring(1, closing), args.substring(closing+1));
		}
		int space = args.indexOf(" ");
		if (space != -1) {
			return result(args.substring(0, space), args.substring(space+1));
		} else if (args.isEmpty()) {
			throw new IllegalArgumentException("Missing argument");
		} else {
			return result(args, "");
		}
	}
	
	List<String> getList() {
		args = args.trim();
		if (!args.startsWith("[")) {
			throw new IllegalArgumentException("'[' expected");
		}
		int closing = args.indexOf(']');
		if (closing == -1) { throw new IllegalArgumentException("Unmatched ']'"); }
		return result(Splitter.on(',').trimResults().splitToList(args.subSequence(1, closing)),
				args.substring(closing+1));
	}
	
	private <T> T result(T value, String remaining) {
		this.args = remaining;
		return value;
	}

	public String getRest() {
		return result(args.trim(), "");
	}

	public boolean tryConsume(String name) {
		if (args.startsWith(name) &&
				(args.length() == name.length() || args.charAt(name.length()) == ' ')) {
			args = args.substring(name.length());
			return true;
		} else {
			return false;
		}
	}

	public int getInt() {
		return Integer.parseInt(getString());
	}
}