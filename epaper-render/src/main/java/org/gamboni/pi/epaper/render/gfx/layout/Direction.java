/**
 * 
 */
package org.gamboni.pi.epaper.render.gfx.layout;

/**
 * @author tendays
 *
 */
public enum Direction {
	RIGHT(1,0),
	UP(0,-1),
	LEFT(-1,0),
	DOWN(0,1);
	
	public final int x, y;
	
	private Direction(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public Direction opposite() {
		switch (this) {
		case RIGHT: return LEFT;
		case UP: return DOWN;
		case LEFT: return RIGHT;
		case DOWN: return UP;
		}
		throw new IllegalArgumentException();
	}
}
