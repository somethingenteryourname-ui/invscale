package dev.invscale.config;

/**
 * Per HUD element settings. When {@link #enabled} is false the element is rendered exactly like vanilla
 * (it ignores the global HUD scale too), so players are never forced to scale the whole HUD.
 */
public final class HudElementSettings {
	public static final double MIN_SCALE = 0.25;
	public static final double MAX_SCALE = 4.0;

	public boolean enabled = true;
	public boolean visible = true;
	public double scale = 1.0;
	public int offsetX = 0;
	public int offsetY = 0;

	public HudElementSettings copy() {
		HudElementSettings copy = new HudElementSettings();
		copy.enabled = this.enabled;
		copy.visible = this.visible;
		copy.scale = this.scale;
		copy.offsetX = this.offsetX;
		copy.offsetY = this.offsetY;
		return copy;
	}

	void normalize() {
		this.scale = ConfigMath.clampScale(this.scale, MIN_SCALE, MAX_SCALE, 1.0);
		this.offsetX = ConfigMath.clampOffset(this.offsetX);
		this.offsetY = ConfigMath.clampOffset(this.offsetY);
	}

	public void resetPosition() {
		this.offsetX = 0;
		this.offsetY = 0;
	}

	public HudElementSettings withScale(double scale) {
		this.scale = scale;
		return this;
	}
}
