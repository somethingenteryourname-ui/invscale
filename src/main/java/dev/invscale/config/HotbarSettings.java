package dev.invscale.config;

/**
 * Hotbar scale and placement. Completely independent from every other scale.
 */
public final class HotbarSettings {
	public static final double MIN_SCALE = 0.25;
	public static final double MAX_SCALE = 5.0;

	public boolean enabled = true;
	public double scale = 1.0;
	public HotbarAnchor anchor = HotbarAnchor.CENTER;
	public int offsetX = 0;
	public int offsetY = 0;

	public HotbarSettings copy() {
		HotbarSettings copy = new HotbarSettings();
		copy.enabled = this.enabled;
		copy.scale = this.scale;
		copy.anchor = this.anchor;
		copy.offsetX = this.offsetX;
		copy.offsetY = this.offsetY;
		return copy;
	}

	void normalize() {
		this.scale = ConfigMath.clampScale(this.scale, MIN_SCALE, MAX_SCALE, 1.0);
		if (this.anchor == null) {
			this.anchor = HotbarAnchor.CENTER;
		}
		this.offsetX = ConfigMath.clampOffset(this.offsetX);
		this.offsetY = ConfigMath.clampOffset(this.offsetY);
	}

	public void resetPosition() {
		this.anchor = HotbarAnchor.CENTER;
		this.offsetX = 0;
		this.offsetY = 0;
	}
}
