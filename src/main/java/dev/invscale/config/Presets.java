package dev.invscale.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import com.google.gson.Gson;

import dev.invscale.hud.HudElementType;
import dev.invscale.scale.ScreenCategory;

/**
 * Built-in presets plus save / load / rename / delete / reset of player presets.
 *
 * <p>A player can save over a built-in preset; that stores an override under the built-in name, and
 * "Reset" removes the override again. Built-in presets themselves can never be deleted or renamed.
 */
public final class Presets {
	public static final String DEFAULT = "Default";
	public static final String PVP = "PvP";
	public static final String CRYSTAL_PVP = "Crystal PvP";
	public static final String MACE_PVP = "Mace PvP";
	public static final String MINIMAL = "Minimal";
	public static final String LARGE_INVENTORY = "Large Inventory";
	public static final String COMPETITIVE = "Competitive";

	public static final int MAX_NAME_LENGTH = 32;

	private static final Gson COMPARE_GSON = new Gson();
	private static final Map<String, Supplier<ScaleProfile>> BUILT_IN = new LinkedHashMap<>();

	static {
		BUILT_IN.put(DEFAULT, ScaleProfile::new);

		// Small, low-profile hotbar; inventory big enough to hotbar-swap quickly.
		BUILT_IN.put(PVP, () -> new ScaleProfile()
				.hotbar(0.85).inventory(1.5).containers(1.25).hudScale(0.9));

		// Exactly the example from the design brief.
		BUILT_IN.put(CRYSTAL_PVP, () -> new ScaleProfile()
				.hotbar(0.8).inventory(3.5).containers(2.5).hudScale(0.9)
				.element(HudElementType.CROSSHAIR, 1.1));

		// Mace players watch their height and the crosshair more than anything else.
		BUILT_IN.put(MACE_PVP, () -> new ScaleProfile()
				.hotbar(0.9).inventory(2.5).containers(2.0).hudScale(0.95)
				.element(HudElementType.CROSSHAIR, 1.25));

		BUILT_IN.put(MINIMAL, () -> {
			ScaleProfile profile = new ScaleProfile().hotbar(0.7).hudScale(0.75)
					.element(HudElementType.CROSSHAIR, 0.85);
			profile.hud(HudElementType.HELD_ITEM_NAME).visible = false;
			return profile;
		});

		BUILT_IN.put(LARGE_INVENTORY, () -> new ScaleProfile()
				.inventory(4.0).containers(3.0)
				.screen(ScreenCategory.LARGE_CHEST, 2.5)
				.screen(ScreenCategory.SHULKER_BOX, 3.5));

		BUILT_IN.put(COMPETITIVE, () -> new ScaleProfile()
				.hotbar(0.75).inventory(2.0).containers(1.75).hudScale(0.85)
				.element(HudElementType.CROSSHAIR, 0.9)
				.element(HudElementType.STATUS_EFFECTS, 0.8));
	}

	private Presets() {
	}

	private static InvScaleConfig config() {
		return ConfigManager.get();
	}

	public static List<String> builtInNames() {
		return new ArrayList<>(BUILT_IN.keySet());
	}

	/** Built-ins first (in their fixed order), then the player's own presets. */
	public static List<String> allNames() {
		List<String> names = builtInNames();
		for (String name : config().userPresets.keySet()) {
			if (!BUILT_IN.containsKey(name)) {
				names.add(name);
			}
		}
		return names;
	}

	public static boolean isBuiltIn(String name) {
		return BUILT_IN.containsKey(name);
	}

	public static boolean hasOverride(String name) {
		return isBuiltIn(name) && config().userPresets.containsKey(name);
	}

	public static boolean exists(String name) {
		return BUILT_IN.containsKey(name) || config().userPresets.containsKey(name);
	}

	/** Case-insensitive lookup for commands; returns the stored spelling or {@code null}. */
	public static String resolveName(String input) {
		String trimmed = sanitizeName(input);
		for (String name : allNames()) {
			if (name.equalsIgnoreCase(trimmed)) {
				return name;
			}
		}
		return null;
	}

	public static ScaleProfile builtIn(String name) {
		Supplier<ScaleProfile> supplier = BUILT_IN.get(name);
		return supplier == null ? null : supplier.get().normalize();
	}

	/** The profile stored for a preset name (player override first), as a fresh copy. */
	public static ScaleProfile get(String name) {
		ScaleProfile user = config().userPresets.get(name);
		if (user != null) {
			return user.copy();
		}
		return builtIn(name);
	}

	public static boolean load(String name) {
		ScaleProfile profile = get(name);
		if (profile == null) {
			return false;
		}
		InvScaleConfig config = config();
		config.current = profile;
		config.activePreset = name;
		ConfigManager.onChanged();
		return true;
	}

	public static Result save(String rawName) {
		String name = sanitizeName(rawName);
		if (name.isEmpty()) {
			return Result.error("invscale.preset.error.empty_name");
		}
		InvScaleConfig config = config();
		config.userPresets.put(name, config.current.copy());
		config.activePreset = name;
		ConfigManager.onChanged();
		return Result.ok(name);
	}

	public static Result rename(String from, String rawTo) {
		String to = sanitizeName(rawTo);
		if (to.isEmpty()) {
			return Result.error("invscale.preset.error.empty_name");
		}
		if (isBuiltIn(from)) {
			return Result.error("invscale.preset.error.builtin_rename");
		}
		if (exists(to) && !to.equals(from)) {
			return Result.error("invscale.preset.error.exists");
		}
		InvScaleConfig config = config();
		ScaleProfile profile = config.userPresets.get(from);
		if (profile == null) {
			return Result.error("invscale.preset.error.not_found");
		}
		// Rebuild to keep insertion order stable.
		Map<String, ScaleProfile> renamed = new LinkedHashMap<>();
		config.userPresets.forEach((name, value) -> renamed.put(name.equals(from) ? to : name, value));
		config.userPresets = renamed;
		if (from.equals(config.activePreset)) {
			config.activePreset = to;
		}
		if (from.equals(config.profileA)) {
			config.profileA = to;
		}
		if (from.equals(config.profileB)) {
			config.profileB = to;
		}
		ConfigManager.onChanged();
		return Result.ok(to);
	}

	public static Result delete(String name) {
		InvScaleConfig config = config();
		if (isBuiltIn(name) && !hasOverride(name)) {
			return Result.error("invscale.preset.error.builtin_delete");
		}
		if (config.userPresets.remove(name) == null) {
			return Result.error("invscale.preset.error.not_found");
		}
		if (!isBuiltIn(name)) {
			if (name.equals(config.profileA)) {
				config.profileA = PVP;
			}
			if (name.equals(config.profileB)) {
				config.profileB = LARGE_INVENTORY;
			}
		}
		ConfigManager.onChanged();
		return Result.ok(name);
	}

	/**
	 * Built-in: drops the player's edits so it is back to its shipped values.
	 * Player preset: resets its stored values to Default.
	 * If the reset preset is the active one, the live settings are reset as well.
	 */
	public static Result reset(String name) {
		InvScaleConfig config = config();
		if (isBuiltIn(name)) {
			config.userPresets.remove(name);
		} else if (config.userPresets.containsKey(name)) {
			config.userPresets.put(name, new ScaleProfile());
		} else {
			return Result.error("invscale.preset.error.not_found");
		}
		if (name.equals(config.activePreset)) {
			config.current = get(name);
		}
		ConfigManager.onChanged();
		return Result.ok(name);
	}

	/** True when the live settings no longer match the active preset. */
	public static boolean isModified() {
		InvScaleConfig config = config();
		ScaleProfile preset = get(config.activePreset);
		if (preset == null) {
			return true;
		}
		return !Objects.equals(COMPARE_GSON.toJson(preset), COMPARE_GSON.toJson(config.current));
	}

	public static String sanitizeName(String name) {
		if (name == null) {
			return "";
		}
		String trimmed = name.strip().replaceAll("\\s+", " ");
		StringBuilder clean = new StringBuilder();
		for (int i = 0; i < trimmed.length() && clean.length() < MAX_NAME_LENGTH; i++) {
			char c = trimmed.charAt(i);
			if (!Character.isISOControl(c) && c != '"' && c != '\\') {
				clean.append(c);
			}
		}
		return clean.toString().strip();
	}

	public static String lowerKey(String name) {
		return name.toLowerCase(Locale.ROOT);
	}

	public record Result(boolean success, String name, String errorKey) {
		static Result ok(String name) {
			return new Result(true, name, null);
		}

		static Result error(String key) {
			return new Result(false, null, key);
		}
	}
}
