package dev.invscale.config;

import net.minecraft.network.chat.Component;

/**
 * Horizontal placement of the hotbar.
 */
public enum HotbarAnchor {
	CENTER("center"),
	LEFT("left"),
	RIGHT("right");

	private final String key;

	HotbarAnchor(String key) {
		this.key = key;
	}

	public Component label() {
		return Component.translatable("invscale.anchor." + this.key);
	}
}
