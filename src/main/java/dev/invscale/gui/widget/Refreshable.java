package dev.invscale.gui.widget;

/**
 * A widget that can re-read its value from the config (after a preset load or a reset).
 */
public interface Refreshable {
	void refresh();
}
