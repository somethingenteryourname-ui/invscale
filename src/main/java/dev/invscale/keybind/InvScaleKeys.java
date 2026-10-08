package dev.invscale.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

import dev.invscale.InvScaleClient;
import dev.invscale.config.ConfigManager;
import dev.invscale.config.InvScaleConfig;
import dev.invscale.config.Presets;

/**
 * Optional keybinds, all configurable in Options -> Controls -> Key Binds under "InvScale".
 */
public final class InvScaleKeys {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(InvScaleClient.MOD_ID, "main"));

	public static KeyMapping openSettings;
	public static KeyMapping togglePvp;
	public static KeyMapping toggleProfiles;

	private InvScaleKeys() {
	}

	public static void register() {
		openSettings = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.invscale.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_I, CATEGORY));
		togglePvp = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.invscale.toggle_pvp", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY));
		toggleProfiles = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.invscale.toggle_profiles", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY));
	}

	public static void tick(Minecraft minecraft) {
		while (openSettings.consumeClick()) {
			if (minecraft.screen == null) {
				InvScaleClient.openConfigScreen(minecraft, null);
			}
		}
		while (togglePvp.consumeClick()) {
			togglePvpPreset(minecraft);
		}
		while (toggleProfiles.consumeClick()) {
			toggleProfiles(minecraft);
		}
	}

	/** Switches to the PvP preset, or back to whatever was active before. */
	public static void togglePvpPreset(Minecraft minecraft) {
		InvScaleConfig config = ConfigManager.get();
		if (config.pvpToggleStash != null) {
			config.current = config.pvpToggleStash;
			config.activePreset = config.pvpToggleStashName == null ? Presets.DEFAULT : config.pvpToggleStashName;
			config.pvpToggleStash = null;
			config.pvpToggleStashName = null;
			ConfigManager.onChanged();
			notify(minecraft, Component.translatable("invscale.message.pvp_off", config.activePreset));
		} else {
			config.pvpToggleStash = config.current.copy();
			config.pvpToggleStashName = config.activePreset;
			Presets.load(Presets.PVP);
			notify(minecraft, Component.translatable("invscale.message.pvp_on"));
		}
	}

	/** Swaps between the two profiles chosen in the Presets / Keybinds tab. */
	public static void toggleProfiles(Minecraft minecraft) {
		InvScaleConfig config = ConfigManager.get();
		String target = config.profileA.equals(config.activePreset) ? config.profileB : config.profileA;
		if (!Presets.exists(target)) {
			notify(minecraft, Component.translatable("invscale.message.missing_preset", target).withStyle(ChatFormatting.RED));
			return;
		}
		config.pvpToggleStash = null;
		config.pvpToggleStashName = null;
		Presets.load(target);
		notify(minecraft, Component.translatable("invscale.message.preset_loaded", target));
	}

	private static void notify(Minecraft minecraft, Component message) {
		if (minecraft.player != null) {
			minecraft.player.displayClientMessage(message, true);
		}
	}
}
