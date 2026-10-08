package dev.invscale.config;

import java.util.LinkedHashMap;
import java.util.Map;

import dev.invscale.hud.HudElementType;
import dev.invscale.scale.ScreenCategory;

/**
 * Everything a preset stores: the four independent scale groups (hotbar, inventory, containers, HUD) plus
 * per-screen overrides, per-HUD-element settings and positions.
 */
public final class ScaleProfile {
	public static final double SCREEN_MIN_SCALE = 0.5;
	public static final double SCREEN_MAX_SCALE = 6.0;
	public static final double HUD_MIN_SCALE = 0.25;
	public static final double HUD_MAX_SCALE = 3.0;

	public HotbarSettings hotbar = new HotbarSettings();
	/** Health, hunger, armor, XP etc. stay attached to the top of the hotbar when it is moved or resized. */
	public boolean barsFollowHotbar = true;

	public boolean inventoryEnabled = true;
	public double inventoryScale = 1.0;

	public boolean containersEnabled = true;
	public double containerScale = 1.0;

	/** Screen category key -> scale. Missing entries use the Inventory/Container scale. */
	public Map<String, Double> screenOverrides = new LinkedHashMap<>();

	public boolean hudEnabled = true;
	public double hudScale = 1.0;
	public Map<String, HudElementSettings> hudElements = new LinkedHashMap<>();

	public ScaleProfile() {
		this.normalize();
	}

	public HudElementSettings hud(HudElementType type) {
		HudElementSettings settings = this.hudElements.get(type.key());
		if (settings == null) {
			settings = new HudElementSettings();
			this.hudElements.put(type.key(), settings);
		}
		return settings;
	}

	/** Returns the override for a screen, or {@code null} when it follows its group default. */
	public Double override(ScreenCategory category) {
		return this.screenOverrides.get(category.key());
	}

	public void setOverride(ScreenCategory category, Double scale) {
		if (scale == null) {
			this.screenOverrides.remove(category.key());
		} else {
			this.screenOverrides.put(category.key(), ConfigMath.clampScale(scale, SCREEN_MIN_SCALE, SCREEN_MAX_SCALE, 1.0));
		}
	}

	/** The scale requested for a category before window-fit limits are applied. */
	public double requestedScale(ScreenCategory category) {
		Double override = this.override(category);
		if (override != null) {
			return override;
		}
		return category.isInventoryGroup() ? this.inventoryScale : this.containerScale;
	}

	public boolean isScalingEnabled(ScreenCategory category) {
		return category.isInventoryGroup() ? this.inventoryEnabled : this.containersEnabled;
	}

	public void resetAllPositions() {
		this.hotbar.resetPosition();
		for (HudElementSettings settings : this.hudElements.values()) {
			settings.resetPosition();
		}
	}

	/** Repairs anything invalid (hand-edited JSON, older versions, missing entries). */
	public ScaleProfile normalize() {
		if (this.hotbar == null) {
			this.hotbar = new HotbarSettings();
		}
		this.hotbar.normalize();
		this.inventoryScale = ConfigMath.clampScale(this.inventoryScale, SCREEN_MIN_SCALE, SCREEN_MAX_SCALE, 1.0);
		this.containerScale = ConfigMath.clampScale(this.containerScale, SCREEN_MIN_SCALE, SCREEN_MAX_SCALE, 1.0);
		this.hudScale = ConfigMath.clampScale(this.hudScale, HUD_MIN_SCALE, HUD_MAX_SCALE, 1.0);

		Map<String, Double> overrides = new LinkedHashMap<>();
		if (this.screenOverrides != null) {
			for (ScreenCategory category : ScreenCategory.values()) {
				Double value = this.screenOverrides.get(category.key());
				if (value != null) {
					overrides.put(category.key(), ConfigMath.clampScale(value, SCREEN_MIN_SCALE, SCREEN_MAX_SCALE, 1.0));
				}
			}
		}
		this.screenOverrides = overrides;

		Map<String, HudElementSettings> elements = new LinkedHashMap<>();
		for (HudElementType type : HudElementType.values()) {
			HudElementSettings settings = this.hudElements == null ? null : this.hudElements.get(type.key());
			if (settings == null) {
				settings = new HudElementSettings();
			}
			settings.normalize();
			elements.put(type.key(), settings);
		}
		this.hudElements = elements;
		return this;
	}

	public ScaleProfile copy() {
		ScaleProfile copy = new ScaleProfile();
		copy.hotbar = this.hotbar.copy();
		copy.barsFollowHotbar = this.barsFollowHotbar;
		copy.inventoryEnabled = this.inventoryEnabled;
		copy.inventoryScale = this.inventoryScale;
		copy.containersEnabled = this.containersEnabled;
		copy.containerScale = this.containerScale;
		copy.screenOverrides = new LinkedHashMap<>(this.screenOverrides);
		copy.hudEnabled = this.hudEnabled;
		copy.hudScale = this.hudScale;
		copy.hudElements = new LinkedHashMap<>();
		this.hudElements.forEach((key, value) -> copy.hudElements.put(key, value.copy()));
		return copy;
	}

	// ---------------------------------------------------------------- fluent helpers used by built-in presets

	ScaleProfile hotbar(double scale) {
		this.hotbar.scale = scale;
		return this;
	}

	ScaleProfile inventory(double scale) {
		this.inventoryScale = scale;
		return this;
	}

	ScaleProfile containers(double scale) {
		this.containerScale = scale;
		return this;
	}

	ScaleProfile hudScale(double scale) {
		this.hudScale = scale;
		return this;
	}

	ScaleProfile element(HudElementType type, double scale) {
		this.hud(type).scale = scale;
		return this;
	}

	ScaleProfile screen(ScreenCategory category, double scale) {
		this.setOverride(category, scale);
		return this;
	}
}
