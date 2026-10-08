package dev.invscale.scale;

import com.mojang.blaze3d.platform.Window;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import dev.invscale.config.ConfigManager;
import dev.invscale.config.InvScaleConfig;
import dev.invscale.config.ScaleMode;
import dev.invscale.config.ScaleProfile;
import dev.invscale.mixin.AbstractContainerScreenAccessor;

/**
 * Decides the scale factor of a container screen. Computed once when the screen is initialised or resized
 * (never per frame) and stored on the screen itself.
 */
public final class ScreenScaler {
	/** Free space (GUI pixels) kept around a scaled screen so it never touches the window edges. */
	private static final int MARGIN = 6;
	/** Creative inventory tabs stick out above and below the main panel. */
	private static final int CREATIVE_TAB_HEIGHT = 28;
	private static final float MIN_FACTOR = 0.25F;

	private ScreenScaler() {
	}

	public static float factorFor(Screen screen, int guiWidth, int guiHeight) {
		InvScaleConfig config = ConfigManager.get();
		if (!config.enabled) {
			return 1.0F;
		}
		ScreenCategory category = ScreenClassifier.classify(screen);
		if (category == null || (category == ScreenCategory.MODDED && !config.scaleModdedScreens)) {
			return 1.0F;
		}
		if (!(screen instanceof AbstractContainerScreenAccessor accessor)) {
			return 1.0F;
		}
		int imageWidth = accessor.invscale$getImageWidth();
		int imageHeight = accessor.invscale$getImageHeight();
		if (category == ScreenCategory.CREATIVE_INVENTORY) {
			imageHeight += CREATIVE_TAB_HEIGHT * 2;
		}
		return compute(config, category, imageWidth, imageHeight, guiWidth, guiHeight).factor();
	}

	/**
	 * @param imageWidth  the container's own width in GUI pixels (176 for most vanilla screens)
	 * @param imageHeight the container's own height in GUI pixels
	 */
	public static Result compute(InvScaleConfig config, ScreenCategory category, int imageWidth, int imageHeight,
			int guiWidth, int guiHeight) {
		ScaleProfile profile = config.current;
		if (!config.enabled || !profile.isScalingEnabled(category)) {
			return new Result(1.0, 1.0F, false);
		}
		double requested = profile.requestedScale(category);
		int guiScale = guiScale();
		double factor = toGuiFactor(config, requested, guiScale);

		// Never let a screen grow past the window: it would become impossible to reach its slots.
		double fit = Math.min(
				guiWidth / (double) Math.max(1, imageWidth + MARGIN * 2),
				guiHeight / (double) Math.max(1, imageHeight + MARGIN * 2));
		double limit = Math.max(1.0, fit);
		boolean limited = false;
		if (factor > 1.0 && factor > limit) {
			factor = limit;
			limited = true;
		}

		if (config.pixelSnap) {
			double pixels = Math.round(factor * guiScale);
			if (factor > 1.0) {
				pixels = Math.min(pixels, Math.floor(limit * guiScale + 1.0E-6));
			}
			factor = Math.max(1.0, pixels) / guiScale;
		}

		float result = (float) Math.max(MIN_FACTOR, factor);
		if (Math.abs(result - 1.0F) < 1.0E-3F) {
			result = 1.0F;
		}
		return new Result(requested, result, limited);
	}

	/** Converts a configured value into a factor relative to the current GUI scale. */
	public static double toGuiFactor(InvScaleConfig config, double value, int guiScale) {
		return config.scaleMode == ScaleMode.ABSOLUTE ? value / Math.max(1, guiScale) : value;
	}

	public static int guiScale() {
		return Math.max(1, Minecraft.getInstance().getWindow().getGuiScale());
	}

	/** Re-lays out an open container screen so config changes (keybinds, commands) apply instantly. */
	public static void onConfigChanged() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null) {
			return;
		}
		Screen screen = minecraft.screen;
		if (screen instanceof AbstractContainerScreen<?>) {
			Window window = minecraft.getWindow();
			screen.resize(window.getGuiScaledWidth(), window.getGuiScaledHeight());
		}
	}

	/**
	 * @param requested the configured value
	 * @param factor    the factor actually used, relative to the GUI scale
	 * @param limited   true when the requested size did not fit the window and was reduced
	 */
	public record Result(double requested, float factor, boolean limited) {
	}
}
