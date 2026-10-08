package dev.invscale.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;

import dev.invscale.scale.ScaleState;
import dev.invscale.scale.ScaledScreen;

/**
 * Draws a scaled screen through a single scale on the GUI pose. The mouse coordinates passed in were already
 * converted by {@code MouseHandlerMixin}, so the screen sees one consistent coordinate system.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@WrapOperation(
			method = "render",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
			)
	)
	private void invscale$renderScaledScreen(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick, Operation<Void> original) {
		float factor = ((ScaledScreen) screen).invscale$getFactor();
		if (factor == 1.0F) {
			original.call(screen, graphics, mouseX, mouseY, partialTick);
			return;
		}
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.scale(factor, factor);
		ScaleState.beginScreenRender(factor, screen.width, screen.height);
		try {
			original.call(screen, graphics, mouseX, mouseY, partialTick);
		} finally {
			ScaleState.endScreenRender();
			pose.popMatrix();
		}
	}
}
