package dev.invscale.gui.widget;

import java.util.function.BooleanSupplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

import dev.invscale.gui.Theme;

/**
 * Flat sidebar tab with an accent bar on the selected category.
 */
public class CategoryButton extends AbstractButton {
	private final Runnable onPress;
	private final BooleanSupplier selected;

	public CategoryButton(int x, int y, int width, int height, Component label, BooleanSupplier selected, Runnable onPress) {
		super(x, y, width, height, label);
		this.onPress = onPress;
		this.selected = selected;
	}

	@Override
	public void onPress(InputWithModifiers input) {
		this.onPress.run();
	}

	@Override
	protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		boolean isSelected = this.selected.getAsBoolean();
		boolean hovered = this.isHoveredOrFocused();
		int x = this.getX();
		int y = this.getY();
		int right = x + this.getWidth();
		int bottom = y + this.getHeight();

		int background = isSelected ? Theme.TAB_SELECTED : hovered ? Theme.TAB_HOVER : Theme.TAB_IDLE;
		graphics.fill(x, y, right, bottom, ARGB.multiplyAlpha(background, this.alpha));
		if (isSelected) {
			graphics.fill(x, y, x + 2, bottom, ARGB.multiplyAlpha(Theme.ACCENT, this.alpha));
		}
		int textColor = isSelected ? Theme.TEXT : hovered ? Theme.TEXT : Theme.TEXT_DIM;
		graphics.drawString(Minecraft.getInstance().font, this.getMessage(), x + 8, y + (this.getHeight() - 8) / 2,
				ARGB.multiplyAlpha(textColor, this.alpha), false);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {
		this.defaultButtonNarrationText(output);
	}
}
