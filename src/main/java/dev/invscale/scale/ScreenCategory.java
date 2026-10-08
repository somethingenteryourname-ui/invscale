package dev.invscale.scale;

import net.minecraft.network.chat.Component;

/**
 * Every kind of container screen that can have its own scale. Player-inventory-type screens fall back to the
 * Inventory Scale, everything else falls back to the Container Scale.
 */
public enum ScreenCategory {
	PLAYER_INVENTORY("player_inventory", true),
	CREATIVE_INVENTORY("creative_inventory", true),
	CHEST("chest", false),
	LARGE_CHEST("large_chest", false),
	CHEST_OTHER("chest_other", false),
	SHULKER_BOX("shulker_box", false),
	CRAFTING_TABLE("crafting_table", false),
	CRAFTER("crafter", false),
	FURNACE("furnace", false),
	BLAST_FURNACE("blast_furnace", false),
	SMOKER("smoker", false),
	ANVIL("anvil", false),
	ENCHANTING_TABLE("enchanting_table", false),
	BREWING_STAND("brewing_stand", false),
	HOPPER("hopper", false),
	DISPENSER("dispenser", false),
	DROPPER("dropper", false),
	LOOM("loom", false),
	STONECUTTER("stonecutter", false),
	SMITHING_TABLE("smithing_table", false),
	GRINDSTONE("grindstone", false),
	CARTOGRAPHY_TABLE("cartography_table", false),
	BEACON("beacon", false),
	MERCHANT("merchant", false),
	MOUNT("mount", false),
	MODDED("modded", false);

	private static final ScreenCategory[] VALUES = values();

	private final String key;
	private final boolean inventoryGroup;

	ScreenCategory(String key, boolean inventoryGroup) {
		this.key = key;
		this.inventoryGroup = inventoryGroup;
	}

	public String key() {
		return this.key;
	}

	/** True when the screen falls back to the Inventory Scale instead of the Container Scale. */
	public boolean isInventoryGroup() {
		return this.inventoryGroup;
	}

	public Component displayName() {
		return Component.translatable("invscale.screen." + this.key);
	}

	public static ScreenCategory byKey(String key) {
		for (ScreenCategory category : VALUES) {
			if (category.key.equals(key)) {
				return category;
			}
		}
		return null;
	}
}
