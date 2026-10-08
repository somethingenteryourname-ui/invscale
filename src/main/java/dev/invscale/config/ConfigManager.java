package dev.invscale.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.loader.api.FabricLoader;

import dev.invscale.scale.ScreenScaler;

/**
 * Loads and saves {@code config/invscale.json}. Everything is client-side; nothing is ever sent to a server.
 */
public final class ConfigManager {
	public static final Logger LOGGER = LoggerFactory.getLogger("InvScale");

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();

	/** Unsaved changes are written this many ticks after the last edit (keybinds, commands, sliders). */
	private static final int AUTOSAVE_DELAY_TICKS = 40;

	private static InvScaleConfig config = new InvScaleConfig().normalize();
	private static boolean dirty;
	private static int ticksSinceChange;
	private static Boolean modifiedCache;

	private ConfigManager() {
	}

	public static InvScaleConfig get() {
		return config;
	}

	public static ScaleProfile profile() {
		return config.current;
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("invscale.json");
	}

	public static void load() {
		Path path = path();
		if (!Files.exists(path)) {
			config = new InvScaleConfig().normalize();
			save();
			return;
		}
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			InvScaleConfig loaded = GSON.fromJson(reader, InvScaleConfig.class);
			config = (loaded == null ? new InvScaleConfig() : loaded).normalize();
		} catch (IOException | JsonParseException | IllegalStateException e) {
			LOGGER.error("[InvScale] Could not read {}, a backup was made and defaults are used", path, e);
			try {
				Files.copy(path, path.resolveSibling("invscale.json.broken"), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException ignored) {
				// Best effort only.
			}
			config = new InvScaleConfig().normalize();
		}
		modifiedCache = null;
	}

	/** Writes the config atomically so a crash mid-write can never corrupt it. */
	public static void save() {
		Path path = path();
		try {
			Files.createDirectories(path.getParent());
			Path temp = path.resolveSibling("invscale.json.tmp");
			try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
				GSON.toJson(config, writer);
			}
			try {
				Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
			}
			dirty = false;
		} catch (IOException e) {
			LOGGER.error("[InvScale] Could not save {}", path, e);
		}
	}

	public static void saveIfDirty() {
		if (dirty) {
			save();
		}
	}

	/** Call after any change to the live config. Applies it instantly; the file is written shortly after. */
	public static void onChanged() {
		dirty = true;
		ticksSinceChange = 0;
		modifiedCache = null;
		ScreenScaler.onConfigChanged();
	}

	public static void tick() {
		if (dirty && ++ticksSinceChange >= AUTOSAVE_DELAY_TICKS) {
			save();
		}
	}

	public static boolean isDirty() {
		return dirty;
	}

	/** Cached "live settings differ from the active preset" check, used by the UI and commands. */
	public static boolean isModifiedFromPreset() {
		if (modifiedCache == null) {
			modifiedCache = Presets.isModified();
		}
		return modifiedCache;
	}

	public static void resetToDefaults() {
		config.current = new ScaleProfile();
		config.activePreset = Presets.DEFAULT;
		onChanged();
	}
}
