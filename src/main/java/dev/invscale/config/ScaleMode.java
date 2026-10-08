package dev.invscale.config;

import net.minecraft.network.chat.Component;

/**
 * How scale values are interpreted.
 */
public enum ScaleMode {
	/** Values multiply the player's current GUI Scale. 1.00x always looks exactly like vanilla. */
	RELATIVE("relative"),
	/** Values are an exact pixel scale (like picking a GUI Scale number), independent of the GUI Scale option. */
	ABSOLUTE("absolute");

	private final String key;

	ScaleMode(String key) {
		this.key = key;
	}

	public Component label() {
		return Component.translatable("invscale.mode." + this.key);
	}

	public Component description() {
		return Component.translatable("invscale.mode." + this.key + ".desc");
	}
}
