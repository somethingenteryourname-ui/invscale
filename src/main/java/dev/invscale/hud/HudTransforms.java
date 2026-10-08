package dev.invscale.hud;

import org.joml.Matrix3x2fStack;

import dev.invscale.config.ConfigManager;
import dev.invscale.config.HotbarSettings;
import dev.invscale.config.HudElementSettings;
import dev.invscale.config.InvScaleConfig;
import dev.invscale.config.ScaleProfile;
import dev.invscale.scale.ScreenScaler;

/**
 * Pure math that places HUD elements. Each element is scaled around its own anchor and then moved, which is
 * one translate + one scale on the GUI pose: effectively free per frame.
 */
public final class HudTransforms {
	/** Vanilla hotbar size in GUI pixels. */
	public static final int HOTBAR_WIDTH = 182;
	public static final int HOTBAR_HEIGHT = 22;
	/** Room kept for the offhand slot when the hotbar is pushed to a screen edge. */
	private static final int OFFHAND_ROOM = 29;
	private static final int EDGE_MARGIN = 4;

	private HudTransforms() {
	}

	private static InvScaleConfig config() {
		return ConfigManager.get();
	}

	public static boolean hotbarActive() {
		InvScaleConfig config = config();
		return config.enabled && config.current.hotbar.enabled;
	}

	/** Hotbar scale relative to the GUI scale (1 = vanilla). */
	public static float hotbarFactor() {
		if (!hotbarActive()) {
			return 1.0F;
		}
		InvScaleConfig config = config();
		return (float) ScreenScaler.toGuiFactor(config, config.current.hotbar.scale, ScreenScaler.guiScale());
	}

	/**
	 * Computes where the hotbar's bottom-center ends up.
	 *
	 * @return {x, y, scale}
	 */
	public static float[] hotbarPlacement(int width, int height) {
		HotbarSettings hotbar = config().current.hotbar;
		float scale = hotbarFactor();
		float half = (HOTBAR_WIDTH / 2.0F + OFFHAND_ROOM) * scale;
		float x = switch (hotbar.anchor) {
			case LEFT -> EDGE_MARGIN + half;
			case RIGHT -> width - EDGE_MARGIN - half;
			default -> width / 2;
		};
		x += hotbar.offsetX;
		float y = height + hotbar.offsetY;

		// Keep the hotbar reachable: its center stays on screen and it never sinks below the bottom edge.
		x = clamp(x, 0, width);
		y = clamp(y, Math.min(HOTBAR_HEIGHT * scale, height), height);
		return new float[] {x, y, scale};
	}

	/** Applies the hotbar transform. Returns false when nothing needs to change (vanilla placement). */
	public static boolean applyHotbar(Matrix3x2fStack pose, int width, int height) {
		if (!hotbarActive()) {
			return false;
		}
		float[] placement = hotbarPlacement(width, height);
		float anchorX = width / 2;
		if (placement[2] == 1.0F && placement[0] == anchorX && placement[1] == height) {
			return false;
		}
		pose.translate(placement[0], placement[1]);
		pose.scale(placement[2], placement[2]);
		pose.translate(-anchorX, -height);
		return true;
	}

	/** Effective scale of a HUD element relative to the GUI scale. */
	public static float elementFactor(HudElementType type) {
		InvScaleConfig config = config();
		ScaleProfile profile = config.current;
		HudElementSettings settings = profile.hud(type);
		double hud = profile.hudEnabled ? ScreenScaler.toGuiFactor(config, profile.hudScale, ScreenScaler.guiScale()) : 1.0;
		return (float) (hud * settings.scale);
	}

	/** Applies the element transform. Returns false when the element is drawn exactly like vanilla. */
	public static boolean applyElement(HudElementType type, Matrix3x2fStack pose, int width, int height) {
		float[] target = elementTarget(type, width, height);
		if (target == null) {
			return false;
		}
		HudElementType.Anchor anchor = type.anchor();
		pose.translate(target[0], target[1]);
		pose.scale(target[2], target[2]);
		pose.translate(-anchor.refX(width), -anchor.refY(height));
		return true;
	}

	/**
	 * Where an element's anchor ends up and at what scale, or {@code null} for vanilla placement.
	 *
	 * @return {x, y, scale}
	 */
	public static float[] elementTarget(HudElementType type, int width, int height) {
		InvScaleConfig config = config();
		ScaleProfile profile = config.current;
		HudElementSettings settings = profile.hud(type);
		if (!config.enabled || !settings.enabled) {
			return null;
		}
		HudElementType.Anchor anchor = type.anchor();
		float refX = anchor.refX(width);
		float refY = anchor.refY(height);
		float x = refX + settings.offsetX;
		float y = refY + settings.offsetY;

		if (type.followsHotbar() && profile.barsFollowHotbar && hotbarActive()) {
			float[] hotbar = hotbarPlacement(width, height);
			x += hotbar[0] - width / 2;
			y += (hotbar[1] - HOTBAR_HEIGHT * hotbar[2]) - (height - HOTBAR_HEIGHT);
		}

		x = clamp(x, 0, width);
		y = clamp(y, 0, height);
		float scale = elementFactor(type);
		if (scale == 1.0F && x == refX && y == refY) {
			return null;
		}
		return new float[] {x, y, scale};
	}

	private static float clamp(float value, float min, float max) {
		return value < min ? min : Math.min(value, max);
	}
}
