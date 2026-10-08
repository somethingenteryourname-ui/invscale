package dev.invscale.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.MouseHandler;

import dev.invscale.scale.ScaleState;

/**
 * Every mouse position and movement delta sent to a screen (clicks, releases, drags, scrolling, hovering and the
 * position used for rendering tooltips) goes through these two static helpers. Converting them into the scaled
 * screen's coordinate system here is the exact inverse of the render transform, so hitboxes always match.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@ModifyReturnValue(method = "getScaledXPos(Lcom/mojang/blaze3d/platform/Window;D)D", at = @At("RETURN"))
	private static double invscale$scaleX(double original) {
		return ScaleState.toScreenSpace(original);
	}

	@ModifyReturnValue(method = "getScaledYPos(Lcom/mojang/blaze3d/platform/Window;D)D", at = @At("RETURN"))
	private static double invscale$scaleY(double original) {
		return ScaleState.toScreenSpace(original);
	}
}
