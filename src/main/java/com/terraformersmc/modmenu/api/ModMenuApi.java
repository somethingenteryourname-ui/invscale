package com.terraformersmc.modmenu.api;

/**
 * Compile-time stub of Mod Menu's API (excluded from the built jar).
 * The real interface is supplied by Mod Menu at runtime; InvScale does not require Mod Menu.
 */
public interface ModMenuApi {
	default ConfigScreenFactory<?> getModConfigScreenFactory() {
		return null;
	}
}
