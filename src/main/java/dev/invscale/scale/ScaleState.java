package dev.invscale.scale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import dev.invscale.config.ConfigManager;
import dev.invscale.config.InvScaleConfig;
import dev.invscale.hud.HudTransforms;

/**
 * Render-thread state shared by the mixins. Only plain field reads happen per frame.
 */
public final class ScaleState {
	private static boolean rendering;
	private static float renderFactor = 1.0F;
	private static int renderWidth;
	private static int renderHeight;

	/** Largest item icon resolution multiplier, keeps the item atlas within sane GPU limits. */
	private static final int MAX_ITEM_PIXEL_SCALE = 16;

	private ScaleState() {
	}

	/** Factor of the currently open screen (1 when nothing is scaled). */
	public static float screenFactor() {
		Screen screen = Minecraft.getInstance().screen;
		return screen instanceof ScaledScreen scaled ? scaled.invscale$getFactor() : 1.0F;
	}

	/** GUI coordinates -> coordinates of the open (possibly scaled) screen. */
	public static double toScreenSpace(double guiCoordinate) {
		float factor = screenFactor();
		return factor == 1.0F ? guiCoordinate : guiCoordinate / factor;
	}

	public static void beginScreenRender(float factor, int virtualWidth, int virtualHeight) {
		rendering = true;
		renderFactor = factor;
		renderWidth = virtualWidth;
		renderHeight = virtualHeight;
	}

	public static void endScreenRender() {
		rendering = false;
		renderFactor = 1.0F;
	}

	/** True only while a scaled screen is being drawn. */
	public static boolean isRenderingScaled() {
		return rendering;
	}

	public static float renderFactor() {
		return renderFactor;
	}

	public static int renderWidth() {
		return renderWidth;
	}

	public static int renderHeight() {
		return renderHeight;
	}

	/** Scales a coordinate from screen space to real GUI space while a scaled screen is being drawn. */
	public static int toGui(int value) {
		return rendering ? Math.round(value * renderFactor) : value;
	}

	public static float toGui(float value) {
		return rendering ? value * renderFactor : value;
	}

	/**
	 * Pixel resolution used for GUI item icons. Vanilla renders them at exactly the GUI scale; when items are shown
	 * bigger (scaled inventory or hotbar) they are rendered sharper instead of being blown up.
	 */
	public static int itemPixelScale(int guiScale) {
		InvScaleConfig config = ConfigManager.get();
		if (!config.enabled || !config.highResItems) {
			return guiScale;
		}
		float largest = Math.max(screenFactor(), HudTransforms.hotbarFactor());
		if (largest <= 1.0F) {
			return guiScale;
		}
		int wanted = (int) Math.ceil(guiScale * largest - 1.0E-4);
		return Math.max(guiScale, Math.min(wanted, MAX_ITEM_PIXEL_SCALE));
	}
}
