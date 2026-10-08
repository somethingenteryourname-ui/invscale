package dev.invscale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import dev.invscale.command.InvScaleCommand;
import dev.invscale.config.ConfigManager;
import dev.invscale.gui.InvScaleScreen;
import dev.invscale.hud.HudScaling;
import dev.invscale.keybind.InvScaleKeys;

/**
 * InvScale - independent hotbar, inventory, container and HUD scaling. Purely client-side and purely visual:
 * it sends no packets and changes no gameplay.
 */
public final class InvScaleClient implements ClientModInitializer {
	public static final String MOD_ID = "invscale";

	private static boolean openRequested;

	@Override
	public void onInitializeClient() {
		ConfigManager.load();
		HudScaling.register();
		InvScaleKeys.register();
		ClientCommandRegistrationCallback.EVENT.register(InvScaleCommand::register);
		ClientTickEvents.END_CLIENT_TICK.register(InvScaleClient::onEndTick);
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.saveIfDirty());
		ConfigManager.LOGGER.info("[InvScale] Loaded - hotbar, inventory, containers and HUD now scale independently");
	}

	private static void onEndTick(Minecraft minecraft) {
		InvScaleKeys.tick(minecraft);
		if (openRequested) {
			openRequested = false;
			openConfigScreen(minecraft, minecraft.screen);
		}
		ConfigManager.tick();
	}

	/**
	 * Opens the settings on the next tick. Needed for the chat command: the chat screen closes itself right after
	 * running a command, which would otherwise close our screen immediately.
	 */
	public static void requestOpen() {
		openRequested = true;
	}

	public static void openConfigScreen(Minecraft minecraft, Screen parent) {
		minecraft.setScreen(new InvScaleScreen(parent));
	}
}
