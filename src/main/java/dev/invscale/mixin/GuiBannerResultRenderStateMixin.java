package dev.invscale.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.render.state.pip.GuiBannerResultRenderState;

import dev.invscale.scale.ScaleState;

/**
 * The loom's banner preview has a hard-coded model scale; scale it with the screen like every other preview.
 */
@Mixin(GuiBannerResultRenderState.class)
public abstract class GuiBannerResultRenderStateMixin {
	@Unique
	private float invscale$factor = 1.0F;

	@Inject(method = "<init>*", at = @At("RETURN"))
	private void invscale$captureFactor(CallbackInfo ci) {
		this.invscale$factor = ScaleState.isRenderingScaled() ? ScaleState.renderFactor() : 1.0F;
	}

	@ModifyReturnValue(method = "scale", at = @At("RETURN"))
	private float invscale$scale(float original) {
		return original * this.invscale$factor;
	}
}
