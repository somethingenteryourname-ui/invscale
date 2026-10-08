package dev.invscale.gui;

import org.joml.Matrix3x2fStack;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import dev.invscale.hud.HudElementType;
import dev.invscale.hud.HudTransforms;
import dev.invscale.scale.ScreenCategory;

/**
 * Live previews drawn by the settings screen. Container previews use the real vanilla textures at the exact
 * size the screen will have; HUD previews (outside a world) use the real HUD sprites and transforms.
 */
public final class PreviewRenderer {
	private static final Identifier HOTBAR = Identifier.withDefaultNamespace("hud/hotbar");
	private static final Identifier HOTBAR_SELECTION = Identifier.withDefaultNamespace("hud/hotbar_selection");
	private static final Identifier CROSSHAIR = Identifier.withDefaultNamespace("hud/crosshair");
	private static final Identifier HEART_CONTAINER = Identifier.withDefaultNamespace("hud/heart/container");
	private static final Identifier HEART_FULL = Identifier.withDefaultNamespace("hud/heart/full");
	private static final Identifier FOOD_EMPTY = Identifier.withDefaultNamespace("hud/food_empty");
	private static final Identifier FOOD_FULL = Identifier.withDefaultNamespace("hud/food_full");
	private static final Identifier ARMOR_FULL = Identifier.withDefaultNamespace("hud/armor_full");
	private static final Identifier XP_BACKGROUND = Identifier.withDefaultNamespace("hud/experience_bar_background");
	private static final Identifier XP_PROGRESS = Identifier.withDefaultNamespace("hud/experience_bar_progress");

	private PreviewRenderer() {
	}

	/** Texture layout of each previewable container. */
	public record ContainerSpec(Identifier texture, int width, int height, int textureWidth, int textureHeight, int chestRows) {
		static ContainerSpec simple(String name) {
			return new ContainerSpec(Identifier.withDefaultNamespace("textures/gui/container/" + name + ".png"), 176, 166, 256, 256, 0);
		}

		static ContainerSpec chest(int rows) {
			return new ContainerSpec(Identifier.withDefaultNamespace("textures/gui/container/generic_54.png"), 176, 114 + rows * 18, 256, 256, rows);
		}
	}

	public static ContainerSpec spec(ScreenCategory category) {
		return switch (category) {
			case PLAYER_INVENTORY, CREATIVE_INVENTORY -> ContainerSpec.simple("inventory");
			case LARGE_CHEST -> ContainerSpec.chest(6);
			case CHEST, CHEST_OTHER, MODDED -> ContainerSpec.chest(3);
			case SHULKER_BOX -> ContainerSpec.simple("shulker_box");
			case CRAFTING_TABLE -> ContainerSpec.simple("crafting_table");
			case CRAFTER -> ContainerSpec.simple("crafter");
			case FURNACE -> ContainerSpec.simple("furnace");
			case BLAST_FURNACE -> ContainerSpec.simple("blast_furnace");
			case SMOKER -> ContainerSpec.simple("smoker");
			case ANVIL -> ContainerSpec.simple("anvil");
			case ENCHANTING_TABLE -> ContainerSpec.simple("enchanting_table");
			case BREWING_STAND -> ContainerSpec.simple("brewing_stand");
			case HOPPER -> new ContainerSpec(Identifier.withDefaultNamespace("textures/gui/container/hopper.png"), 176, 133, 256, 256, 0);
			case DISPENSER, DROPPER -> ContainerSpec.simple("dispenser");
			case LOOM -> ContainerSpec.simple("loom");
			case STONECUTTER -> ContainerSpec.simple("stonecutter");
			case SMITHING_TABLE -> ContainerSpec.simple("smithing");
			case GRINDSTONE -> ContainerSpec.simple("grindstone");
			case CARTOGRAPHY_TABLE -> ContainerSpec.simple("cartography_table");
			case BEACON -> new ContainerSpec(Identifier.withDefaultNamespace("textures/gui/container/beacon.png"), 230, 219, 256, 256, 0);
			case MERCHANT -> new ContainerSpec(Identifier.withDefaultNamespace("textures/gui/container/villager.png"), 276, 166, 512, 256, 0);
			case MOUNT -> ContainerSpec.simple("horse");
		};
	}

	/** Draws a container background at {@code scale}, top-left at (x, y). */
	public static void drawContainer(GuiGraphics graphics, ContainerSpec spec, float x, float y, float scale) {
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale(scale, scale);
		if (spec.chestRows() > 0) {
			int top = spec.chestRows() * 18 + 17;
			graphics.blit(RenderPipelines.GUI_TEXTURED, spec.texture(), 0, 0, 0.0F, 0.0F, spec.width(), top, 256, 256);
			graphics.blit(RenderPipelines.GUI_TEXTURED, spec.texture(), 0, top, 0.0F, 126.0F, spec.width(), 96, 256, 256);
		} else {
			graphics.blit(RenderPipelines.GUI_TEXTURED, spec.texture(), 0, 0, 0.0F, 0.0F, spec.width(), spec.height(),
					spec.textureWidth(), spec.textureHeight());
		}
		pose.popMatrix();
	}

	/**
	 * Mock HUD for when the settings are opened from the main menu (there is no real HUD to look at then).
	 */
	public static void drawMockHud(GuiGraphics graphics, int width, int height) {
		Matrix3x2fStack pose = graphics.pose();
		int center = width / 2;

		pose.pushMatrix();
		HudTransforms.applyHotbar(pose, width, height);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR, center - 91, height - 22, 182, 22);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SELECTION, center - 91 - 1, height - 22 - 1, 24, 23);
		pose.popMatrix();

		pose.pushMatrix();
		HudTransforms.applyElement(HudElementType.XP_BAR, pose, width, height);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, XP_BACKGROUND, center - 91, height - 29, 182, 5);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, XP_PROGRESS, 182, 5, 0, 0, center - 91, height - 29, 120, 5);
		pose.popMatrix();

		if (visible(HudElementType.HEALTH)) {
			pose.pushMatrix();
			HudTransforms.applyElement(HudElementType.HEALTH, pose, width, height);
			for (int i = 0; i < 10; i++) {
				int x = center - 91 + i * 8;
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_CONTAINER, x, height - 39, 9, 9);
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEART_FULL, x, height - 39, 9, 9);
			}
			pose.popMatrix();
		}

		if (visible(HudElementType.ARMOR)) {
			pose.pushMatrix();
			HudTransforms.applyElement(HudElementType.ARMOR, pose, width, height);
			for (int i = 0; i < 10; i++) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARMOR_FULL, center - 91 + i * 8, height - 49, 9, 9);
			}
			pose.popMatrix();
		}

		if (visible(HudElementType.HUNGER)) {
			pose.pushMatrix();
			HudTransforms.applyElement(HudElementType.HUNGER, pose, width, height);
			for (int i = 0; i < 10; i++) {
				int x = center + 91 - i * 8 - 9;
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FOOD_EMPTY, x, height - 39, 9, 9);
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FOOD_FULL, x, height - 39, 9, 9);
			}
			pose.popMatrix();
		}

		if (visible(HudElementType.CROSSHAIR)) {
			pose.pushMatrix();
			HudTransforms.applyElement(HudElementType.CROSSHAIR, pose, width, height);
			graphics.blitSprite(RenderPipelines.CROSSHAIR, CROSSHAIR, (width - 15) / 2, (height - 15) / 2, 15, 15);
			pose.popMatrix();
		}
	}

	private static boolean visible(HudElementType type) {
		var settings = dev.invscale.config.ConfigManager.profile().hud(type);
		return !settings.enabled || settings.visible;
	}

	/** Pulsing outline around the transformed hotbar, so players can see exactly where it ends up. */
	public static void outlineHotbar(GuiGraphics graphics, int width, int height, float pulse) {
		float[] placement = HudTransforms.hotbarPlacement(width, height);
		float scale = placement[2];
		int left = Math.round(placement[0] - 91 * scale) - 2;
		int right = Math.round(placement[0] + 91 * scale) + 2;
		int top = Math.round(placement[1] - 22 * scale) - 2;
		int bottom = Math.round(placement[1]) + 1;
		int color = ARGB.multiplyAlpha(Theme.ACCENT, 0.45F + 0.45F * pulse);
		graphics.renderOutline(left, top, right - left, bottom - top, color);
	}

	/** Small cross-hair style marker on the anchor of a HUD element. */
	public static void markAnchor(GuiGraphics graphics, int x, int y, float pulse) {
		int color = ARGB.multiplyAlpha(Theme.ACCENT, 0.5F + 0.5F * pulse);
		graphics.fill(x - 6, y, x + 7, y + 1, color);
		graphics.fill(x, y - 6, x + 1, y + 7, color);
		graphics.renderOutline(x - 3, y - 3, 7, 7, color);
	}
}
