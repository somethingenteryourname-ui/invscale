package dev.invscale.command;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import java.util.function.BiConsumer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import dev.invscale.InvScaleClient;
import dev.invscale.config.ConfigManager;
import dev.invscale.config.ConfigMath;
import dev.invscale.config.HotbarSettings;
import dev.invscale.config.InvScaleConfig;
import dev.invscale.config.Presets;
import dev.invscale.config.ScaleProfile;

/**
 * {@code /invscale} - a purely client-side command (registered with Fabric's client command API, never sent to
 * the server).
 *
 * <pre>
 * /invscale                       open the settings screen
 * /invscale reset                 reset everything to Default
 * /invscale preset &lt;name&gt;         load a preset
 * /invscale presets               list presets
 * /invscale save &lt;name&gt;           save the current settings as a preset
 * /invscale hotbar|inventory|containers|hud &lt;scale&gt;
 * /invscale toggle                turn InvScale on/off
 * /invscale status                show the current scales
 * /invscale reload                reload the config file
 * </pre>
 */
public final class InvScaleCommand {
	private static final SuggestionProvider<FabricClientCommandSource> PRESET_SUGGESTIONS =
			(context, builder) -> SharedSuggestionProvider.suggest(Presets.allNames(), builder);

	private InvScaleCommand() {
	}

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext buildContext) {
		LiteralArgumentBuilder<FabricClientCommandSource> root = literal("invscale")
				.executes(context -> {
					InvScaleClient.requestOpen();
					return 1;
				})
				.then(literal("reset").executes(context -> {
					ConfigManager.resetToDefaults();
					ConfigManager.save();
					success(context, Component.translatable("invscale.command.reset"));
					return 1;
				}))
				.then(literal("presets").executes(InvScaleCommand::listPresets))
				.then(literal("preset")
						.then(argument("name", StringArgumentType.greedyString())
								.suggests(PRESET_SUGGESTIONS)
								.executes(InvScaleCommand::loadPreset)))
				.then(literal("save")
						.then(argument("name", StringArgumentType.greedyString())
								.suggests(PRESET_SUGGESTIONS)
								.executes(InvScaleCommand::savePreset)))
				.then(scaleCommand("hotbar", HotbarSettings.MIN_SCALE, HotbarSettings.MAX_SCALE,
						(profile, value) -> profile.hotbar.scale = value))
				.then(scaleCommand("inventory", ScaleProfile.SCREEN_MIN_SCALE, ScaleProfile.SCREEN_MAX_SCALE,
						(profile, value) -> profile.inventoryScale = value))
				.then(scaleCommand("containers", ScaleProfile.SCREEN_MIN_SCALE, ScaleProfile.SCREEN_MAX_SCALE,
						(profile, value) -> profile.containerScale = value))
				.then(scaleCommand("hud", ScaleProfile.HUD_MIN_SCALE, ScaleProfile.HUD_MAX_SCALE,
						(profile, value) -> profile.hudScale = value))
				.then(literal("toggle").executes(context -> {
					InvScaleConfig config = ConfigManager.get();
					config.enabled = !config.enabled;
					ConfigManager.onChanged();
					ConfigManager.save();
					success(context, Component.translatable(config.enabled ? "invscale.command.enabled" : "invscale.command.disabled"));
					return 1;
				}))
				.then(literal("status").executes(InvScaleCommand::status))
				.then(literal("reload").executes(context -> {
					ConfigManager.load();
					ConfigManager.onChanged();
					success(context, Component.translatable("invscale.command.reloaded"));
					return 1;
				}));
		dispatcher.register(root);
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> scaleCommand(String name, double min, double max,
			BiConsumer<ScaleProfile, Double> setter) {
		return literal(name).then(argument("scale", DoubleArgumentType.doubleArg(min, max)).executes(context -> {
			double value = ConfigMath.round2(DoubleArgumentType.getDouble(context, "scale"));
			setter.accept(ConfigManager.profile(), value);
			ConfigManager.profile().normalize();
			ConfigManager.onChanged();
			ConfigManager.save();
			success(context, Component.translatable("invscale.command.set", Component.translatable("invscale.group." + name),
					ConfigMath.formatScale(value)));
			return 1;
		}));
	}

	private static int loadPreset(CommandContext<FabricClientCommandSource> context) {
		String input = StringArgumentType.getString(context, "name");
		String name = Presets.resolveName(input);
		if (name == null) {
			context.getSource().sendError(Component.translatable("invscale.preset.error.unknown", input));
			return 0;
		}
		Presets.load(name);
		ConfigManager.get().pvpToggleStash = null;
		ConfigManager.save();
		success(context, Component.translatable("invscale.message.preset_loaded", name));
		return 1;
	}

	private static int savePreset(CommandContext<FabricClientCommandSource> context) {
		Presets.Result result = Presets.save(StringArgumentType.getString(context, "name"));
		if (!result.success()) {
			context.getSource().sendError(Component.translatable(result.errorKey()));
			return 0;
		}
		ConfigManager.save();
		success(context, Component.translatable("invscale.command.saved", result.name()));
		return 1;
	}

	private static int listPresets(CommandContext<FabricClientCommandSource> context) {
		InvScaleConfig config = ConfigManager.get();
		MutableComponent list = Component.translatable("invscale.command.presets").withStyle(ChatFormatting.GOLD);
		for (String name : Presets.allNames()) {
			MutableComponent entry = Component.literal("\n  " + name);
			if (Presets.isBuiltIn(name)) {
				entry.append(Component.literal(Presets.hasOverride(name) ? " *" : "").withStyle(ChatFormatting.GRAY));
			}
			entry.withStyle(name.equals(config.activePreset) ? ChatFormatting.AQUA : ChatFormatting.WHITE);
			list.append(entry);
		}
		context.getSource().sendFeedback(list);
		return 1;
	}

	private static int status(CommandContext<FabricClientCommandSource> context) {
		InvScaleConfig config = ConfigManager.get();
		ScaleProfile profile = config.current;
		String preset = config.activePreset + (ConfigManager.isModifiedFromPreset() ? " (modified)" : "");
		context.getSource().sendFeedback(Component.translatable("invscale.command.status",
				preset,
				ConfigMath.formatScale(profile.hotbar.scale),
				ConfigMath.formatScale(profile.inventoryScale),
				ConfigMath.formatScale(profile.containerScale),
				ConfigMath.formatScale(profile.hudScale),
				config.scaleMode.label()).withStyle(ChatFormatting.GRAY));
		return 1;
	}

	private static void success(CommandContext<FabricClientCommandSource> context, Component message) {
		context.getSource().sendFeedback(Component.literal("[InvScale] ").withStyle(ChatFormatting.AQUA).append(message.copy().withStyle(ChatFormatting.WHITE)));
	}
}
