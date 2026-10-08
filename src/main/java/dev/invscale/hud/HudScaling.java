package dev.invscale.hud;

import org.joml.Matrix3x2fStack;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

import dev.invscale.config.ConfigManager;
import dev.invscale.config.HudElementSettings;
import dev.invscale.config.InvScaleConfig;

/**
 * Wraps vanilla HUD layers through Fabric's HUD API. Vanilla still draws everything; InvScale only changes the
 * transform around each layer, which keeps it compatible with Sodium, PvP HUD mods and other HUD API users.
 */
public final class HudScaling {
	private HudScaling() {
	}

	public static void register() {
		HudElementRegistry.replaceElement(VanillaHudElements.HOTBAR, HudScaling::wrapHotbar);
		HudElementRegistry.replaceElement(VanillaHudElements.SPECTATOR_MENU, HudScaling::wrapHotbar);

		for (HudElementType type : HudElementType.values()) {
			for (Identifier layer : type.vanillaLayers()) {
				HudElementRegistry.replaceElement(layer, original -> wrapElement(type, original));
			}
		}
	}

	private static HudElement wrapHotbar(HudElement original) {
		return (graphics, deltaTracker) -> renderHotbar(original, graphics, deltaTracker);
	}

	private static void renderHotbar(HudElement original, GuiGraphics graphics, DeltaTracker deltaTracker) {
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		try {
			HudTransforms.applyHotbar(pose, graphics.guiWidth(), graphics.guiHeight());
			original.render(graphics, deltaTracker);
		} finally {
			pose.popMatrix();
		}
	}

	private static HudElement wrapElement(HudElementType type, HudElement original) {
		return (graphics, deltaTracker) -> renderElement(type, original, graphics, deltaTracker);
	}

	private static void renderElement(HudElementType type, HudElement original, GuiGraphics graphics, DeltaTracker deltaTracker) {
		InvScaleConfig config = ConfigManager.get();
		HudElementSettings settings = config.current.hud(type);
		if (config.enabled && settings.enabled && !settings.visible) {
			return;
		}
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		try {
			HudTransforms.applyElement(type, pose, graphics.guiWidth(), graphics.guiHeight());
			original.render(graphics, deltaTracker);
		} finally {
			pose.popMatrix();
		}
	}
}
