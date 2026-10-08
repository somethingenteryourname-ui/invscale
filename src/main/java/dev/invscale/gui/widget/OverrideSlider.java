package dev.invscale.gui.widget;

import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import dev.invscale.config.ConfigMath;

/**
 * Per-screen scale slider. The far left notch means "Default" (follow the Inventory/Container scale), the rest of
 * the track picks an explicit scale for that one screen.
 */
public class OverrideSlider extends AbstractSliderButton implements Refreshable {
	/** Portion of the track reserved for "Default". */
	private static final double DEFAULT_ZONE = 0.06;

	private final double min;
	private final double max;
	private final double step;
	private final Supplier<Double> getter;
	private final Consumer<Double> setter;
	private final DoubleSupplier fallback;

	public OverrideSlider(int x, int y, int width, int height, double min, double max, double step,
			Supplier<Double> getter, Consumer<Double> setter, DoubleSupplier fallback) {
		super(x, y, width, height, Component.empty(), 0.0);
		this.min = min;
		this.max = max;
		this.step = step;
		this.getter = getter;
		this.setter = setter;
		this.fallback = fallback;
		this.refresh();
	}

	private Double toReal(double slider) {
		if (slider < DEFAULT_ZONE) {
			return null;
		}
		double t = (slider - DEFAULT_ZONE) / (1.0 - DEFAULT_ZONE);
		double raw = this.min + t * (this.max - this.min);
		return ConfigMath.round2(Mth.clamp(Math.round(raw / this.step) * this.step, this.min, this.max));
	}

	private double toSlider(Double real) {
		if (real == null) {
			return 0.0;
		}
		double t = Mth.clamp((real - this.min) / (this.max - this.min), 0.0, 1.0);
		return DEFAULT_ZONE + t * (1.0 - DEFAULT_ZONE);
	}

	@Override
	protected void updateMessage() {
		Double real = this.toReal(this.value);
		if (real == null) {
			this.setMessage(Component.translatable("invscale.value.default", ConfigMath.formatScale(this.fallback.getAsDouble()))
					.withStyle(ChatFormatting.GRAY));
		} else {
			this.setMessage(Component.literal(ConfigMath.formatScale(real)));
		}
	}

	@Override
	protected void applyValue() {
		this.setter.accept(this.toReal(this.value));
	}

	@Override
	public void refresh() {
		this.value = this.toSlider(this.getter.get());
		this.updateMessage();
	}

	@Override
	public void onRelease(MouseButtonEvent event) {
		super.onRelease(event);
		this.value = this.toSlider(this.toReal(this.value));
	}
}
