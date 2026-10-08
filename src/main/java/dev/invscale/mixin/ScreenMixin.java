package dev.invscale.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.client.gui.screens.Screen;

import dev.invscale.scale.ScaledScreen;
import dev.invscale.scale.ScreenScaler;

/**
 * Gives a scaled screen a smaller (or larger) virtual size. The screen lays itself out normally inside that
 * size, so it stays perfectly centred, and {@code GameRendererMixin} / {@code MouseHandlerMixin} apply the
 * matching render transform and mouse transform.
 */
@Mixin(Screen.class)
public abstract class ScreenMixin implements ScaledScreen {
	@Unique
	private float invscale$factor = 1.0F;

	@Override
	public float invscale$getFactor() {
		return this.invscale$factor;
	}

	@WrapMethod(method = "init(II)V")
	private void invscale$scaledInit(int width, int height, Operation<Void> original) {
		this.invscale$factor = ScreenScaler.factorFor((Screen) (Object) this, width, height);
		original.call(this.invscale$virtual(width), this.invscale$virtual(height));
	}

	@WrapMethod(method = "resize(II)V")
	private void invscale$scaledResize(int width, int height, Operation<Void> original) {
		this.invscale$factor = ScreenScaler.factorFor((Screen) (Object) this, width, height);
		original.call(this.invscale$virtual(width), this.invscale$virtual(height));
	}

	@Unique
	private int invscale$virtual(int size) {
		return this.invscale$factor == 1.0F ? size : Math.max(1, (int) Math.floor(size / this.invscale$factor));
	}
}
