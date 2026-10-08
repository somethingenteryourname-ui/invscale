package dev.invscale.scale;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractMountInventoryScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CrafterScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.inventory.ChestMenu;

/**
 * Maps a screen to its {@link ScreenCategory}. Only container screens are ever scaled; menus, chat, signs,
 * books and every other screen keep vanilla behaviour. Results are cached per screen class.
 */
public final class ScreenClassifier {
	/** Marker for classes that are never scaled (ConcurrentHashMap cannot store null). */
	private static final Object NONE = new Object();
	/** Marker for classes whose category depends on the instance (chest rows, dispenser vs dropper). */
	private static final Object DYNAMIC = new Object();

	private static final Map<Class<?>, Object> CACHE = new ConcurrentHashMap<>();

	private ScreenClassifier() {
	}

	public static ScreenCategory classify(Screen screen) {
		if (!(screen instanceof AbstractContainerScreen<?> container)) {
			return null;
		}
		Object cached = CACHE.computeIfAbsent(screen.getClass(), ScreenClassifier::classifyClass);
		if (cached == NONE) {
			return null;
		}
		if (cached == DYNAMIC) {
			return classifyDynamic(container);
		}
		return (ScreenCategory) cached;
	}

	private static Object classifyClass(Class<?> type) {
		// Order matters: most specific first.
		if (CreativeModeInventoryScreen.class.isAssignableFrom(type)) return ScreenCategory.CREATIVE_INVENTORY;
		if (InventoryScreen.class.isAssignableFrom(type)) return ScreenCategory.PLAYER_INVENTORY;
		if (ContainerScreen.class.isAssignableFrom(type)) return DYNAMIC;
		if (DispenserScreen.class.isAssignableFrom(type)) return DYNAMIC;
		if (ShulkerBoxScreen.class.isAssignableFrom(type)) return ScreenCategory.SHULKER_BOX;
		if (CraftingScreen.class.isAssignableFrom(type)) return ScreenCategory.CRAFTING_TABLE;
		if (CrafterScreen.class.isAssignableFrom(type)) return ScreenCategory.CRAFTER;
		if (BlastFurnaceScreen.class.isAssignableFrom(type)) return ScreenCategory.BLAST_FURNACE;
		if (SmokerScreen.class.isAssignableFrom(type)) return ScreenCategory.SMOKER;
		if (FurnaceScreen.class.isAssignableFrom(type)) return ScreenCategory.FURNACE;
		if (AnvilScreen.class.isAssignableFrom(type)) return ScreenCategory.ANVIL;
		if (SmithingScreen.class.isAssignableFrom(type)) return ScreenCategory.SMITHING_TABLE;
		if (EnchantmentScreen.class.isAssignableFrom(type)) return ScreenCategory.ENCHANTING_TABLE;
		if (BrewingStandScreen.class.isAssignableFrom(type)) return ScreenCategory.BREWING_STAND;
		if (HopperScreen.class.isAssignableFrom(type)) return ScreenCategory.HOPPER;
		if (LoomScreen.class.isAssignableFrom(type)) return ScreenCategory.LOOM;
		if (StonecutterScreen.class.isAssignableFrom(type)) return ScreenCategory.STONECUTTER;
		if (GrindstoneScreen.class.isAssignableFrom(type)) return ScreenCategory.GRINDSTONE;
		if (CartographyTableScreen.class.isAssignableFrom(type)) return ScreenCategory.CARTOGRAPHY_TABLE;
		if (BeaconScreen.class.isAssignableFrom(type)) return ScreenCategory.BEACON;
		if (MerchantScreen.class.isAssignableFrom(type)) return ScreenCategory.MERCHANT;
		if (AbstractMountInventoryScreen.class.isAssignableFrom(type)) return ScreenCategory.MOUNT;
		if (AbstractContainerScreen.class.isAssignableFrom(type)) return ScreenCategory.MODDED;
		return NONE;
	}

	private static ScreenCategory classifyDynamic(AbstractContainerScreen<?> screen) {
		if (screen instanceof ContainerScreen chest) {
			ChestMenu menu = chest.getMenu();
			int rows = menu.getRowCount();
			if (rows >= 6) {
				return ScreenCategory.LARGE_CHEST;
			}
			boolean enderOrBarrel = isTitle(screen, "container.enderchest") || isTitle(screen, "container.barrel");
			return rows == 3 && !enderOrBarrel ? ScreenCategory.CHEST : ScreenCategory.CHEST_OTHER;
		}
		if (screen instanceof DispenserScreen) {
			return isTitle(screen, "container.dropper") ? ScreenCategory.DROPPER : ScreenCategory.DISPENSER;
		}
		return ScreenCategory.MODDED;
	}

	private static boolean isTitle(Screen screen, String translationKey) {
		return screen.getTitle().getContents() instanceof TranslatableContents contents
				&& translationKey.equals(contents.getKey());
	}
}
