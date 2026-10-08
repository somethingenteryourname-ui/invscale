package dev.invscale.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("imageWidth")
	int invscale$getImageWidth();

	@Accessor("imageHeight")
	int invscale$getImageHeight();

	@Accessor("leftPos")
	int invscale$getLeftPos();

	@Accessor("topPos")
	int invscale$getTopPos();

	@Accessor("hoveredSlot")
	Slot invscale$getHoveredSlot();
}
