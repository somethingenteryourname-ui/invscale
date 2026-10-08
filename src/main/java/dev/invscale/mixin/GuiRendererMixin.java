package dev.invscale.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.gui.render.GuiRenderer;

import dev.invscale.scale.ScaleState;

/**
 * GUI item icons are pre-rendered into an atlas at exactly the GUI scale. When InvScale shows items larger
 * (big inventory or hotbar) the atlas resolution is raised to match so items stay sharp instead of pixelated.
 * Vanilla already rebuilds the atlas whenever this value changes.
 */
@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
	@ModifyExpressionValue(
			method = "getGuiScaleInvalidatingItemAtlasIfChanged",
			at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;getGuiScale()I")
	)
	private int invscale$itemPixelScale(int guiScale) {
		return ScaleState.itemPixelScale(guiScale);
	}
}
