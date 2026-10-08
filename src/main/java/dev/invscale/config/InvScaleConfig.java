package dev.invscale.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Root of {@code config/invscale.json}.
 */
public final class InvScaleConfig {
	public static final int CURRENT_VERSION = 1;

	public int configVersion = CURRENT_VERSION;

	/** Master switch. When off, InvScale renders everything exactly like vanilla. */
	public boolean enabled = true;
	public ScaleMode scaleMode = ScaleMode.RELATIVE;
	/** Rounds screen scales to whole pixels for perfectly crisp text and textures. */
	public boolean pixelSnap = false;
	/** Renders item icons at a higher resolution when they are drawn larger than normal. */
	public boolean highResItems = true;
	/** Applies the Container Scale to container screens added by other mods. */
	public boolean scaleModdedScreens = true;

	/** The live settings. */
	public ScaleProfile current = new ScaleProfile();

	/** Player-made presets, and player edits of built-in presets (stored under the built-in name). */
	public Map<String, ScaleProfile> userPresets = new LinkedHashMap<>();

	public String activePreset = Presets.DEFAULT;
	public String profileA = Presets.PVP;
	public String profileB = Presets.LARGE_INVENTORY;

	/** Settings stashed by the "Toggle PvP preset" key so the toggle can restore them. Not a preset. */
	public ScaleProfile pvpToggleStash = null;
	public String pvpToggleStashName = null;

	public InvScaleConfig normalize() {
		this.configVersion = CURRENT_VERSION;
		if (this.scaleMode == null) {
			this.scaleMode = ScaleMode.RELATIVE;
		}
		if (this.current == null) {
			this.current = new ScaleProfile();
		}
		this.current.normalize();

		Map<String, ScaleProfile> presets = new LinkedHashMap<>();
		if (this.userPresets != null) {
			this.userPresets.forEach((name, profile) -> {
				String clean = Presets.sanitizeName(name);
				if (!clean.isEmpty() && profile != null) {
					presets.put(clean, profile.normalize());
				}
			});
		}
		this.userPresets = presets;

		if (this.pvpToggleStash != null) {
			this.pvpToggleStash.normalize();
		}
		if (this.activePreset == null) {
			this.activePreset = Presets.DEFAULT;
		}
		if (this.profileA == null) {
			this.profileA = Presets.PVP;
		}
		if (this.profileB == null) {
			this.profileB = Presets.LARGE_INVENTORY;
		}
		return this;
	}
}
