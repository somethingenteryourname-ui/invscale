package dev.invscale.config;

/**
 * Small helpers used to sanitize values loaded from disk or typed by the player.
 */
public final class ConfigMath {
	/** Largest offset (in GUI pixels) an element may be moved. Rendering additionally keeps elements on screen. */
	public static final int MAX_OFFSET = 1000;

	private ConfigMath() {
	}

	public static double clampScale(double value, double min, double max, double fallback) {
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			return fallback;
		}
		return round2(Math.max(min, Math.min(max, value)));
	}

	public static int clampOffset(int value) {
		return Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, value));
	}

	/** Rounds to two decimals so values shown as "1.25x" are stored exactly like that. */
	public static double round2(double value) {
		return Math.round(value * 100.0) / 100.0;
	}

	public static String formatScale(double value) {
		return String.format(java.util.Locale.ROOT, "%.2fx", value);
	}
}
