package dev.invscale.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import dev.invscale.gui.InvScaleScreen;

/**
 * Adds a "Config" button for InvScale to Mod Menu. Only loaded when Mod Menu is installed.
 */
public final class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return InvScaleScreen::new;
	}
}
