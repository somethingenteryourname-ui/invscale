package dev.invscale.test;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.mixin.client.gametest.input.MouseHandlerAccessor;

import dev.invscale.config.ConfigManager;
import dev.invscale.config.HotbarAnchor;
import dev.invscale.config.Presets;
import dev.invscale.config.ScaleProfile;
import dev.invscale.gui.InvScaleScreen;
import dev.invscale.hud.HudElementType;
import dev.invscale.keybind.InvScaleKeys;
import dev.invscale.mixin.AbstractContainerScreenAccessor;
import dev.invscale.scale.ScaledScreen;

/**
 * Runs inside a real Minecraft 1.21.11 client on CI (production jar, intermediary names). It drives the real
 * mouse and keyboard to prove that scaled screens have matching hitboxes, and takes screenshots of everything.
 */
public final class InvScaleClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(client -> {
			client.options.guiScale().set(2);
			ConfigManager.resetToDefaults();
		});
		context.getInput().resizeWindow(1920, 1080);
		context.waitTicks(10);

		// Settings screen from the main menu (mock HUD preview)
		InvScaleScreen.selectCategory(InvScaleScreen.Category.HOTBAR);
		context.setScreen(() -> new InvScaleScreen(null));
		context.waitTicks(5);
		context.takeScreenshot("00_menu_from_title_hotbar");
		context.setScreen(() -> null);
		context.waitTicks(5);

		try (TestSingleplayerContext singleplayer = context.worldBuilder()
				.adjustSettings(creator -> creator.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL))
				.create()) {
			singleplayer.getClientWorld().waitForChunksRender();
			singleplayer.getServer().runCommand("give @a minecraft:diamond_sword");
			singleplayer.getServer().runCommand("give @a minecraft:golden_apple 16");
			singleplayer.getServer().runCommand("give @a minecraft:end_crystal 64");
			singleplayer.getServer().runCommand("give @a minecraft:obsidian 64");
			singleplayer.getServer().runCommand("effect give @a minecraft:speed 999 1");
			singleplayer.getServer().runCommand("effect give @a minecraft:strength 999 1");
			context.waitTicks(20);
			context.takeScreenshot("01_hud_vanilla");

			testInventory(context);
			testLargeChest(context, singleplayer);
			testHud(context);
			testCommandsAndKeybind(context);
			testMenus(context);
			testPreset(context);

			context.runOnClient(client -> ConfigManager.resetToDefaults());
		}
	}

	// ------------------------------------------------------------------------------------------ inventory

	private static void testInventory(ClientGameTestContext context) {
		context.runOnClient(client -> {
			ScaleProfile profile = new ScaleProfile();
			profile.inventoryScale = 2.5;
			ConfigManager.get().current = profile;
			ConfigManager.onChanged();
		});
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitForScreen(InventoryScreen.class);
		context.waitTicks(3);

		float factor = context.computeOnClient(client -> ((ScaledScreen) client.screen).invscale$getFactor());
		check(Math.abs(factor - 2.5F) < 0.01F, "inventory factor should be 2.5 but was " + factor);
		int virtualWidth = context.computeOnClient(client -> client.screen.width);
		int guiWidth = context.computeOnClient(client -> client.getWindow().getGuiScaledWidth());
		check(virtualWidth == (int) Math.floor(guiWidth / 2.5), "virtual width " + virtualWidth + " for gui width " + guiWidth);

		// Hover: the hotbar slot holding the sword (menu slot 36) must be the hovered slot.
		moveToSlot(context, 36);
		context.waitTicks(2);
		int hovered = context.computeOnClient(InvScaleClientGameTest::hoveredSlotIndex);
		check(hovered == 36, "hovered slot should be 36 but was " + hovered);
		context.takeScreenshot("02_inventory_2.5x_tooltip");

		// Click-pick-up and click-place.
		clickSlot(context, 36);
		ItemStack carried = context.computeOnClient(client -> client.player.containerMenu.getCarried().copy());
		check(carried.is(Items.DIAMOND_SWORD), "picked up " + carried);
		clickSlot(context, 9);
		check(context.computeOnClient(client -> client.player.getInventory().getItem(9).is(Items.DIAMOND_SWORD)),
				"sword should now be in inventory slot 9");
		check(context.computeOnClient(client -> client.player.containerMenu.getCarried().isEmpty()), "cursor should be empty");

		// Shift-click the golden apples (menu slot 37 = hotbar slot 1) into the main inventory.
		shiftClickSlot(context, 37);
		check(context.computeOnClient(client -> client.player.getInventory().getItem(1).isEmpty()), "shift-click should move hotbar slot 1");
		check(context.computeOnClient(client -> client.player.getInventory().countItem(Items.GOLDEN_APPLE) == 16), "golden apples lost");

		// Armor slot hover (menu slot 6 = chestplate).
		moveToSlot(context, 6);
		context.waitTicks(2);
		check(context.computeOnClient(InvScaleClientGameTest::hoveredSlotIndex) == 6, "armor slot hover mismatch");

		// Crafting grid: put the sword into crafting slot 1 and take it back.
		clickSlot(context, 9);
		clickSlot(context, 1);
		check(context.computeOnClient(client -> client.player.containerMenu.getSlot(1).getItem().is(Items.DIAMOND_SWORD)), "crafting slot click failed");
		clickSlot(context, 1);
		clickSlot(context, 9);
		context.takeScreenshot("03_inventory_after_clicks");

		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);
	}

	// ------------------------------------------------------------------------------------------ chest

	private static void testLargeChest(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		context.runOnClient(client -> {
			ConfigManager.profile().containerScale = 2.0;
			ConfigManager.onChanged();
		});
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			player.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.sixRows(id, inventory),
					Component.literal("InvScale Test Chest")));
		});
		context.waitForScreen(ContainerScreen.class);
		context.waitTicks(3);
		float factor = context.computeOnClient(client -> ((ScaledScreen) client.screen).invscale$getFactor());
		check(Math.abs(factor - 2.0F) < 0.01F, "large chest factor should be 2.0 but was " + factor);

		// Player inventory slot 9 is chest-menu slot 54. Move the sword into chest slot 0.
		clickSlot(context, 54);
		clickSlot(context, 0);
		check(context.computeOnClient(client -> client.player.containerMenu.getSlot(0).getItem().is(Items.DIAMOND_SWORD)),
				"sword should be in chest slot 0");
		moveToSlot(context, 0);
		context.waitTicks(2);
		context.takeScreenshot("04_large_chest_2x");
		// And back.
		clickSlot(context, 0);
		clickSlot(context, 54);
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);
	}

	// ------------------------------------------------------------------------------------------ HUD

	private static void testHud(ClientGameTestContext context) {
		context.runOnClient(client -> {
			ScaleProfile profile = ConfigManager.profile();
			profile.hotbar.scale = 0.6;
			profile.hudScale = 0.8;
			profile.hud(HudElementType.CROSSHAIR).scale = 2.0;
			ConfigManager.onChanged();
		});
		context.waitTicks(3);
		context.takeScreenshot("05_hud_hotbar_0.6_crosshair_2x");

		context.runOnClient(client -> {
			ConfigManager.profile().hotbar.anchor = HotbarAnchor.LEFT;
			ConfigManager.profile().hotbar.scale = 1.5;
			ConfigManager.profile().hud(HudElementType.STATUS_EFFECTS).scale = 1.5;
			ConfigManager.onChanged();
		});
		context.waitTicks(3);
		context.takeScreenshot("06_hud_hotbar_left_1.5x");
	}

	// ------------------------------------------------------------------------------------------ commands & keys

	private static void testCommandsAndKeybind(ClientGameTestContext context) {
		runChatCommand(context, "/invscale hud 1.5");
		check(context.computeOnClient(client -> ConfigManager.profile().hudScale == 1.5), "/invscale hud 1.5 did not apply");

		runChatCommand(context, "/invscale");
		context.waitForScreen(InvScaleScreen.class);
		context.setScreen(() -> null);
		context.waitTicks(2);

		context.getInput().pressKey(InvScaleKeys.openSettings);
		context.waitForScreen(InvScaleScreen.class);
		context.setScreen(() -> null);
		context.waitTicks(2);

		runChatCommand(context, "/invscale reset");
		check(context.computeOnClient(client -> ConfigManager.profile().hotbar.scale == 1.0), "/invscale reset did not apply");
	}

	private static void runChatCommand(ClientGameTestContext context, String command) {
		context.getInput().pressKey(options -> options.keyChat);
		context.waitTicks(2);
		context.getInput().typeChars(command);
		context.getInput().holdKeyFor(InputConstants.KEY_RETURN, 0);
		context.waitTicks(3);
	}

	// ------------------------------------------------------------------------------------------ menu screenshots

	private static void testMenus(ClientGameTestContext context) {
		context.runOnClient(client -> {
			ConfigManager.profile().hotbar.scale = 0.8;
			ConfigManager.profile().inventoryScale = 3.0;
			ConfigManager.profile().containerScale = 2.0;
			ConfigManager.onChanged();
		});
		int index = 10;
		for (InvScaleScreen.Category category : InvScaleScreen.Category.values()) {
			InvScaleScreen.selectCategory(category);
			context.setScreen(() -> new InvScaleScreen(null));
			context.waitTicks(4);
			context.takeScreenshot(index++ + "_menu_" + category.name().toLowerCase(java.util.Locale.ROOT));
		}
		context.setScreen(() -> null);
		context.waitTicks(2);
	}

	// ------------------------------------------------------------------------------------------ presets

	private static void testPreset(ClientGameTestContext context) {
		context.runOnClient(client -> Presets.load(Presets.CRYSTAL_PVP));
		context.waitTicks(3);
		context.takeScreenshot("20_crystal_pvp_hud");
		context.getInput().pressKey(options -> options.keyInventory);
		context.waitForScreen(InventoryScreen.class);
		context.waitTicks(3);
		float factor = context.computeOnClient(client -> ((ScaledScreen) client.screen).invscale$getFactor());
		check(factor > 1.0F && factor <= 3.5F, "crystal pvp inventory factor " + factor);
		moveToSlot(context, 9);
		context.waitTicks(2);
		check(context.computeOnClient(InvScaleClientGameTest::hoveredSlotIndex) == 9, "hover mismatch at factor " + factor);
		context.takeScreenshot("21_crystal_pvp_inventory");
		context.getInput().pressKey(InputConstants.KEY_ESCAPE);
		context.waitForScreen(null);
	}

	// ------------------------------------------------------------------------------------------ helpers

	private static int hoveredSlotIndex(Minecraft client) {
		if (!(client.screen instanceof AbstractContainerScreen<?> screen)) {
			return -2;
		}
		Slot slot = ((AbstractContainerScreenAccessor) screen).invscale$getHoveredSlot();
		return slot == null ? -1 : slot.index;
	}

	/** Puts the real OS cursor on the center of a slot, going through the full scaled transform. */
	private static void moveToSlot(ClientGameTestContext context, int slotIndex) {
		double[] position = context.computeOnClient(client -> {
			AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.screen;
			AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
			float factor = ((ScaledScreen) screen).invscale$getFactor();
			Slot slot = screen.getMenu().getSlot(slotIndex);
			double virtualX = accessor.invscale$getLeftPos() + slot.x + 8;
			double virtualY = accessor.invscale$getTopPos() + slot.y + 8;
			Window window = client.getWindow();
			double guiX = virtualX * factor;
			double guiY = virtualY * factor;
			return new double[] {
					guiX * window.getScreenWidth() / window.getGuiScaledWidth(),
					guiY * window.getScreenHeight() / window.getGuiScaledHeight()
			};
		});
		context.getInput().setCursorPos(position[0], position[1]);
	}

	private static void clickSlot(ClientGameTestContext context, int slotIndex) {
		moveToSlot(context, slotIndex);
		context.waitTicks(1);
		context.getInput().pressMouse(0);
		context.waitTicks(3);
	}

	/**
	 * Shift-click through the real mouse handler. The game test input API sends clicks without modifier keys, so
	 * the shift modifier is passed explicitly, exactly like GLFW does for a real shift-click.
	 */
	private static void shiftClickSlot(ClientGameTestContext context, int slotIndex) {
		moveToSlot(context, slotIndex);
		context.waitTicks(1);
		context.runOnClient(client -> {
			MouseHandlerAccessor mouse = (MouseHandlerAccessor) client.mouseHandler;
			long window = client.getWindow().handle();
			mouse.invokeOnMouseButton(window, new MouseButtonInfo(0, GLFW.GLFW_MOD_SHIFT), GLFW.GLFW_PRESS);
			mouse.invokeOnMouseButton(window, new MouseButtonInfo(0, GLFW.GLFW_MOD_SHIFT), GLFW.GLFW_RELEASE);
		});
		context.waitTicks(3);
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError("[InvScale test] " + message);
		}
	}
}
