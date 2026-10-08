package dev.invscale.scale;

/**
 * Implemented by every {@link net.minecraft.client.gui.screens.Screen} through {@code ScreenMixin}.
 *
 * <p>The factor maps the screen's own (virtual) coordinate system to normal GUI coordinates:
 * {@code gui = virtual * factor}. Rendering is scaled by the factor and mouse input is divided by it, so what is
 * drawn and what is clickable always use the exact same transform.
 */
public interface ScaledScreen {
	float invscale$getFactor();
}
