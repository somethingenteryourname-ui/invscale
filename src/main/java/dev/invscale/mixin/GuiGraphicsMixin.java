package dev.invscale.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.client.gui.GuiGraphics;

import dev.invscale.scale.ScaleState;

/**
 * Keeps the few GuiGraphics features that bypass the pose matrix consistent while a scaled screen is drawn:
 * tooltip placement (screen bounds), scissor hit-tests and picture-in-picture renders (player model, enchanting
 * book, signs, banners), which are positioned in absolute GUI coordinates by vanilla.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
	// ------------------------------------------------------------------ screen size seen by tooltips

	@ModifyReturnValue(method = "guiWidth", at = @At("RETURN"))
	private int invscale$virtualWidth(int original) {
		return ScaleState.isRenderingScaled() ? ScaleState.renderWidth() : original;
	}

	@ModifyReturnValue(method = "guiHeight", at = @At("RETURN"))
	private int invscale$virtualHeight(int original) {
		return ScaleState.isRenderingScaled() ? ScaleState.renderHeight() : original;
	}

	// ------------------------------------------------------------------ scissor hit-tests (scroll lists)

	@ModifyVariable(method = "containsPointInScissor", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int invscale$scissorX(int x) {
		return ScaleState.toGui(x);
	}

	@ModifyVariable(method = "containsPointInScissor", at = @At("HEAD"), argsOnly = true, ordinal = 1)
	private int invscale$scissorY(int y) {
		return ScaleState.toGui(y);
	}

	// ------------------------------------------------------------------ picture-in-picture bounds

	@ModifyVariable(
			method = {"submitEntityRenderState", "submitSkinRenderState", "submitBookModelRenderState", "submitBannerPatternRenderState", "submitSignRenderState"},
			at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int invscale$pipX0(int value) {
		return ScaleState.toGui(value);
	}

	@ModifyVariable(
			method = {"submitEntityRenderState", "submitSkinRenderState", "submitBookModelRenderState", "submitBannerPatternRenderState", "submitSignRenderState"},
			at = @At("HEAD"), argsOnly = true, ordinal = 1)
	private int invscale$pipY0(int value) {
		return ScaleState.toGui(value);
	}

	@ModifyVariable(
			method = {"submitEntityRenderState", "submitSkinRenderState", "submitBookModelRenderState", "submitBannerPatternRenderState", "submitSignRenderState"},
			at = @At("HEAD"), argsOnly = true, ordinal = 2)
	private int invscale$pipX1(int value) {
		return ScaleState.toGui(value);
	}

	@ModifyVariable(
			method = {"submitEntityRenderState", "submitSkinRenderState", "submitBookModelRenderState", "submitBannerPatternRenderState", "submitSignRenderState"},
			at = @At("HEAD"), argsOnly = true, ordinal = 3)
	private int invscale$pipY1(int value) {
		return ScaleState.toGui(value);
	}

	/** The first float argument of these methods is the model's pixel scale. */
	@ModifyVariable(
			method = {"submitEntityRenderState", "submitSkinRenderState", "submitBookModelRenderState", "submitSignRenderState"},
			at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float invscale$pipScale(float scale) {
		return ScaleState.toGui(scale);
	}
}
