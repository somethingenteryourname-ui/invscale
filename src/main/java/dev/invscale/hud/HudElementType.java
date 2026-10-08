package dev.invscale.hud;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

/**
 * The HUD elements InvScale can scale, move and hide. Each one wraps one or more vanilla HUD layers registered
 * through Fabric's {@code HudElementRegistry}, so no vanilla rendering code is replaced.
 */
public enum HudElementType {
	CROSSHAIR("crosshair", Anchor.CENTER, VanillaHudElements.CROSSHAIR),
	HEALTH("health", Anchor.ABOVE_HOTBAR, VanillaHudElements.HEALTH_BAR),
	ARMOR("armor", Anchor.ABOVE_HOTBAR, VanillaHudElements.ARMOR_BAR),
	HUNGER("hunger", Anchor.ABOVE_HOTBAR, VanillaHudElements.FOOD_BAR),
	AIR("air", Anchor.ABOVE_HOTBAR, VanillaHudElements.AIR_BAR),
	MOUNT_HEALTH("mount_health", Anchor.ABOVE_HOTBAR, VanillaHudElements.MOUNT_HEALTH),
	XP_BAR("xp_bar", Anchor.ABOVE_HOTBAR, VanillaHudElements.INFO_BAR, VanillaHudElements.EXPERIENCE_LEVEL),
	HELD_ITEM_NAME("held_item_name", Anchor.ABOVE_HOTBAR, VanillaHudElements.HELD_ITEM_TOOLTIP),
	ACTION_BAR("action_bar", Anchor.ABOVE_HOTBAR, VanillaHudElements.OVERLAY_MESSAGE),
	STATUS_EFFECTS("status_effects", Anchor.TOP_RIGHT, VanillaHudElements.STATUS_EFFECTS),
	BOSS_BAR("boss_bar", Anchor.TOP_CENTER, VanillaHudElements.BOSS_BAR),
	SCOREBOARD("scoreboard", Anchor.RIGHT_CENTER, VanillaHudElements.SCOREBOARD),
	TITLE("title", Anchor.CENTER, VanillaHudElements.TITLE_AND_SUBTITLE),
	PLAYER_LIST("player_list", Anchor.TOP_CENTER, VanillaHudElements.PLAYER_LIST);

	private static final HudElementType[] VALUES = values();

	private final String key;
	private final Anchor anchor;
	private final List<Identifier> vanillaLayers;

	HudElementType(String key, Anchor anchor, Identifier... vanillaLayers) {
		this.key = key;
		this.anchor = anchor;
		this.vanillaLayers = List.of(vanillaLayers);
	}

	public String key() {
		return this.key;
	}

	public Anchor anchor() {
		return this.anchor;
	}

	public List<Identifier> vanillaLayers() {
		return this.vanillaLayers;
	}

	/** Elements stacked above the hotbar can optionally follow the hotbar when it is moved or resized. */
	public boolean followsHotbar() {
		return this.anchor == Anchor.ABOVE_HOTBAR;
	}

	public Component displayName() {
		return Component.translatable("invscale.hud." + this.key);
	}

	public static HudElementType byKey(String key) {
		for (HudElementType type : VALUES) {
			if (type.key.equals(key)) {
				return type;
			}
		}
		return null;
	}

	/**
	 * Reference point each element is scaled around. Using the element's natural anchor keeps it in its vanilla
	 * position when scaled, e.g. status effects stay glued to the top-right corner.
	 */
	public enum Anchor {
		CENTER,
		TOP_CENTER,
		TOP_RIGHT,
		RIGHT_CENTER,
		/** Top-center of the vanilla hotbar: health, hunger, armor, XP, held item name, action bar... */
		ABOVE_HOTBAR;

		public float refX(int width) {
			return switch (this) {
				case TOP_RIGHT, RIGHT_CENTER -> width;
				default -> width / 2;
			};
		}

		public float refY(int height) {
			return switch (this) {
				case CENTER, RIGHT_CENTER -> height / 2.0F;
				case TOP_CENTER, TOP_RIGHT -> 0.0F;
				case ABOVE_HOTBAR -> height - 22;
			};
		}
	}
}
