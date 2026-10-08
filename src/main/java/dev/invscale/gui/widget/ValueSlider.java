package dev.invscale.gui.widget;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * A slider bound to a numeric setting. The value snaps to a step and is shown exactly (e.g. "1.25x"), and every
 * movement is applied immediately for live preview. Arrow keys move it one step at a time.
 */
public class ValueSlider extends AbstractSliderButton implements Refreshable {
	protected final double min;
	protected final double max;
	protected final double step;
	private final DoubleSupplier getter;
	private final DoubleConsumer setter;
	private final DoubleFunction<Component> formatter;

	public ValueSlider(int x, int y, int width, int height, double min, double max, double step,
			DoubleSupplier getter, DoubleConsumer setter, DoubleFunction<Component> formatter) {
		super(x, y, width, height, Component.empty(), 0.0);
		this.min = min;
		this.max = max;
		this.step = step;
		this.getter = getter;
		this.setter = setter;
		this.formatter = formatter;
		this.refresh();
	}

	protected double toSlider(double real) {
		return Mth.clamp((real - this.min) / (this.max - this.min), 0.0, 1.0);
	}

	protected double toReal(double slider) {
		double raw = this.min + Mth.clamp(slider, 0.0, 1.0) * (this.max - this.min);
		double snapped = Math.round(raw / this.step) * this.step;
		return Math.round(Mth.clamp(snapped, this.min, this.max) * 100.0) / 100.0;
	}

	public double realValue() {
		return this.toReal(this.value);
	}

	@Override
	protected void updateMessage() {
		this.setMessage(this.formatter.apply(this.realValue()));
	}

	@Override
	protected void applyValue() {
		this.setter.accept(this.realValue());
	}

	@Override
	public void refresh() {
		this.value = this.toSlider(this.getter.getAsDouble());
		this.updateMessage();
	}

	/** Moves the slider by whole steps (used by the arrow keys). */
	public void nudge(int steps) {
		this.setValue(this.toSlider(this.realValue() + steps * this.step));
	}

	@Override
	public void onRelease(MouseButtonEvent event) {
		super.onRelease(event);
		// Snap the handle onto the value that is actually stored.
		this.value = this.toSlider(this.realValue());
	}
}
