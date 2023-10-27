/**
 * 
 */
package org.gamboni.pi.epaper.render.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author tendays
 *
 */
public abstract class WidgetHolder<T> {
	private final List<T> widgets = new ArrayList<>();
	
	public void add(T face) {
		this.widgets.add(face);
	}
	
	protected void renderWidgets(Consumer<T> renderer) {
		List<String> errors = new ArrayList<>();
		widgets.forEach(t -> {
			try {
				renderer.accept(t);
			} catch (Throwable x) {
				x.printStackTrace();
				errors.add(t.getClass().getSimpleName() +": "+ x);
			}
			});
		if (!errors.isEmpty()) {
			throw new RuntimeException(
					errors.stream().collect(Collectors.joining(";")));
		}
	}
}
