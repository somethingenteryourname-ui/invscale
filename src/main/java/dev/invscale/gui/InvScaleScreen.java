package dev.invscale.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import dev.invscale.config.ConfigManager;
import dev.invscale.config.ConfigMath;
import dev.invscale.config.HotbarAnchor;
import dev.invscale.config.HotbarSettings;
import dev.invscale.config.HudElementSettings;
import dev.invscale.config.InvScaleConfig;
import dev.invscale.config.Presets;
import dev.invscale.config.ScaleMode;
import dev.invscale.config.ScaleProfile;
import dev.invscale.gui.widget.CategoryButton;
import dev.invscale.gui.widget.OverrideSlider;
import dev.invscale.gui.widget.Refreshable;
import dev.invscale.gui.widget.ValueSlider;
import dev.invscale.hud.HudElementType;
import dev.invscale.hud.HudTransforms;
import dev.invscale.keybind.InvScaleKeys;
import dev.invscale.scale.ScreenCategory;
import dev.invscale.scale.ScreenScaler;

/**
 * The {@code /invscale} settings screen.
 *
 * <p>All edits apply to the live config immediately, so the real HUD behind this screen and the container
 * previews update while sliders move. While a HUD slider is being dragged the whole menu fades out so the
 * result can be judged without the menu in the way.
 */
public class InvScaleScreen extends Screen {
	public enum Category {
		GENERAL, HOTBAR, INVENTORY, CONTAINERS, HUD, POSITIONS, PRESETS, KEYBINDS;

		public Component label() {
			return Component.translatable("invscale.category." + this.name().toLowerCase(Locale.ROOT));
		}

		public Component description() {
			return Component.translatable("invscale.category." + this.name().toLowerCase(Locale.ROOT) + ".desc");
		}

		boolean showsHud() {
			return this == HOTBAR || this == HUD || this == POSITIONS;
		}
	}

	private static final int HEADER_HEIGHT = 24;
	private static final int FOOTER_HEIGHT = 28;
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_GAP = 4;
	private static final int TAB_HEIGHT = 16;
	private static final int PADDING = 8;
	private static final double SCALE_STEP = 0.05;

	/** Remembered between openings so the menu reopens where the player left it. */
	private static Category lastCategory = Category.GENERAL;
	private static int positionTarget;
	private static @Nullable String selectedPreset;
	private static ScreenCategory previewCategory = ScreenCategory.CHEST;

	private final @Nullable Screen parent;
	private Category category;

	private int sidebarX;
	private int sidebarWidth;
	private int panelX;
	private int panelWidth;
	private int panelTop;
	private int panelBottom;
	private int viewTop;
	private int viewBottom;
	private int innerX;
	private int innerWidth;
	private int labelWidth;
	private int fieldX;
	private int fieldWidth;
	private int previewLeft;

	private final List<Placed> placed = new ArrayList<>();
	private final List<Label> labels = new ArrayList<>();
	private final List<DynamicLabel> dynamicLabels = new ArrayList<>();
	private final List<Refreshable> refreshables = new ArrayList<>();
	private int cursor;
	private int contentHeight;
	private double scroll;

	private @Nullable AbstractWidget dragging;
	private float fade = 1.0F;
	private boolean rebuildPending;

	private @Nullable Button resetButton;
	private long resetArmedUntil;
	private @Nullable EditBox presetNameBox;

	private @Nullable Component toast;
	private int toastColor;
	private long toastUntil;

	private record Placed(AbstractWidget widget, int contentY) {
	}

	private record Label(FormattedCharSequence text, int x, int contentY, int color) {
	}

	private record DynamicLabel(Supplier<Component> text, int x, int contentY, int color, boolean alignRight) {
	}

	public InvScaleScreen(@Nullable Screen parent) {
		super(Component.translatable("invscale.title"));
		this.parent = parent;
		this.category = lastCategory;
	}

	private static InvScaleConfig config() {
		return ConfigManager.get();
	}

	private static ScaleProfile profile() {
		return ConfigManager.profile();
	}

	// =================================================================================================== layout

	private void computeLayout() {
		this.sidebarX = 6;
		this.sidebarWidth = this.width >= 420 ? 96 : 82;
		this.panelX = this.sidebarX + this.sidebarWidth + 6;
		this.panelWidth = Mth.clamp(this.width - this.panelX - 6, 180, 300);
		this.panelTop = HEADER_HEIGHT + 6;
		this.panelBottom = this.height - FOOTER_HEIGHT - 2;
		this.viewTop = this.panelTop + 24;
		this.viewBottom = this.panelBottom - 4;
		this.innerX = this.panelX + PADDING;
		this.innerWidth = this.panelWidth - PADDING * 2 - 4;
		this.labelWidth = Math.round(this.innerWidth * 0.42F);
		this.fieldX = this.innerX + this.labelWidth;
		this.fieldWidth = this.innerWidth - this.labelWidth;
		this.previewLeft = this.panelX + this.panelWidth + 8;
	}

	@Override
	protected void init() {
		this.computeLayout();
		this.placed.clear();
		this.labels.clear();
		this.dynamicLabels.clear();
		this.refreshables.clear();
		this.presetNameBox = null;
		this.dragging = null;

		Category[] categories = Category.values();
		for (int i = 0; i < categories.length; i++) {
			Category tab = categories[i];
			this.addRenderableWidget(new CategoryButton(this.sidebarX + 3, this.panelTop + 4 + i * (TAB_HEIGHT + 2),
					this.sidebarWidth - 6, TAB_HEIGHT, tab.label(), () -> this.category == tab, () -> this.switchCategory(tab)));
		}

		int buttonWidth = (this.panelWidth - 8) / 3;
		int footerY = this.height - FOOTER_HEIGHT + 4;
		this.resetButton = this.addRenderableWidget(Button.builder(this.resetLabel(), button -> this.onResetPressed())
				.bounds(this.panelX, footerY, buttonWidth, 20)
				.tooltip(Tooltip.create(Component.translatable("invscale.button.reset.tooltip")))
				.build());
		this.addRenderableWidget(Button.builder(Component.translatable("invscale.button.apply"), button -> this.apply())
				.bounds(this.panelX + buttonWidth + 4, footerY, buttonWidth, 20)
				.tooltip(Tooltip.create(Component.translatable("invscale.button.apply.tooltip")))
				.build());
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
				.bounds(this.panelX + (buttonWidth + 4) * 2, footerY, buttonWidth, 20)
				.build());

		this.cursor = 0;
		switch (this.category) {
			case GENERAL -> this.buildGeneral();
			case HOTBAR -> this.buildHotbar();
			case INVENTORY -> this.buildInventory();
			case CONTAINERS -> this.buildContainers();
			case HUD -> this.buildHud();
			case POSITIONS -> this.buildPositions();
			case PRESETS -> this.buildPresets();
			case KEYBINDS -> this.buildKeybinds();
		}
		this.contentHeight = this.cursor + 4;
		this.setScroll(this.scroll);
	}

	private void switchCategory(Category next) {
		if (this.category != next) {
			this.category = next;
			lastCategory = next;
			this.scroll = 0;
			this.resetArmedUntil = 0;
			this.rebuildPending = true;
		}
	}

	private int viewHeight() {
		return this.viewBottom - this.viewTop;
	}

	private void setScroll(double value) {
		double max = Math.max(0, this.contentHeight - this.viewHeight());
		this.scroll = Mth.clamp(value, 0, max);
		for (Placed entry : this.placed) {
			int y = this.viewTop + entry.contentY() - (int) this.scroll;
			entry.widget().setY(y);
			entry.widget().visible = y >= this.viewTop && y + entry.widget().getHeight() <= this.viewBottom;
		}
	}

	// =================================================================================================== row building

	private <T extends AbstractWidget> T place(T widget) {
		this.placed.add(new Placed(widget, this.cursor));
		this.addRenderableWidget(widget);
		if (widget instanceof Refreshable refreshable) {
			this.refreshables.add(refreshable);
		}
		return widget;
	}

	private void heading(Component text) {
		if (this.cursor > 0) {
			this.cursor += 6;
		}
		this.labels.add(new Label(text.copy().withStyle(ChatFormatting.BOLD).getVisualOrderText(), this.innerX, this.cursor + 2, Theme.HEADING));
		this.cursor += 14;
	}

	private <T extends AbstractWidget> T row(Component label, T widget) {
		this.labels.add(new Label(this.fit(label, this.labelWidth - 4), this.innerX, this.cursor + 6, Theme.TEXT_DIM));
		widget.setX(this.fieldX);
		widget.setWidth(this.fieldWidth);
		this.place(widget);
		this.cursor += ROW_HEIGHT + ROW_GAP;
		return widget;
	}

	private void fullRow(AbstractWidget... widgets) {
		int gap = 4;
		int width = (this.innerWidth - gap * (widgets.length - 1)) / widgets.length;
		for (int i = 0; i < widgets.length; i++) {
			widgets[i].setX(this.innerX + i * (width + gap));
			widgets[i].setWidth(width);
			this.place(widgets[i]);
		}
		this.cursor += ROW_HEIGHT + ROW_GAP;
	}

	private void text(Component text, int color) {
		for (FormattedCharSequence line : this.font.split(text, this.innerWidth)) {
			this.labels.add(new Label(line, this.innerX, this.cursor, color));
			this.cursor += 10;
		}
		this.cursor += 2;
	}

	private void dynamicText(Supplier<Component> text, int color) {
		this.dynamicLabels.add(new DynamicLabel(text, this.innerX, this.cursor, color, false));
		this.cursor += 12;
	}

	private FormattedCharSequence fit(Component text, int maxWidth) {
		List<FormattedCharSequence> lines = this.font.split(text, Math.max(10, maxWidth));
		return lines.isEmpty() ? FormattedCharSequence.EMPTY : lines.get(0);
	}

	/** Big "HOTBAR SCALE ........ 1.00x" title, full width slider and quick-pick buttons. */
	private ValueSlider scaleBlock(String titleKey, double min, double max, DoubleSupplier getter, DoubleConsumer setter, double... quickPicks) {
		if (this.cursor > 0) {
			this.cursor += 4;
		}
		this.labels.add(new Label(Component.translatable(titleKey).withStyle(ChatFormatting.BOLD).getVisualOrderText(),
				this.innerX, this.cursor + 2, Theme.TEXT));
		this.dynamicLabels.add(new DynamicLabel(() -> Component.literal(ConfigMath.formatScale(getter.getAsDouble())).withStyle(ChatFormatting.BOLD),
				this.innerX + this.innerWidth, this.cursor + 2, Theme.VALUE, true));
		this.cursor += 14;

		ValueSlider slider = new ValueSlider(this.innerX, 0, this.innerWidth, ROW_HEIGHT, min, max, SCALE_STEP, getter,
				value -> {
					setter.accept(value);
					this.changed();
				},
				value -> Component.literal(ConfigMath.formatScale(value)));
		this.place(slider);
		this.cursor += ROW_HEIGHT + ROW_GAP;

		if (quickPicks.length > 0) {
			AbstractWidget[] buttons = new AbstractWidget[quickPicks.length];
			for (int i = 0; i < quickPicks.length; i++) {
				double pick = quickPicks[i];
				buttons[i] = Button.builder(Component.literal(ConfigMath.formatScale(pick)), button -> {
					setter.accept(pick);
					this.changed();
					slider.refresh();
				}).bounds(0, 0, 40, ROW_HEIGHT).build();
			}
			this.fullRow(buttons);
		}
		return slider;
	}

	private CycleButton<Boolean> toggle(boolean initial, Consumer<Boolean> setter) {
		return CycleButton.onOffBuilder(initial)
				.create(0, 0, 100, ROW_HEIGHT, Component.empty(), (button, value) -> {
					setter.accept(value);
					this.changed();
				});
	}

	private CycleButton<Boolean> toggle(boolean initial, Component tooltip, Consumer<Boolean> setter) {
		CycleButton<Boolean> button = this.toggle(initial, setter);
		button.setTooltip(Tooltip.create(tooltip));
		return button;
	}

	private ValueSlider offsetSlider(int range, DoubleSupplier getter, Consumer<Integer> setter) {
		return new ValueSlider(0, 0, 100, ROW_HEIGHT, -range, range, 1.0, getter,
				value -> {
					setter.accept((int) Math.round(value));
					this.changed();
				},
				value -> Component.literal((value > 0 ? "+" : "") + (int) Math.round(value) + " px"));
	}

	private Button button(Component label, Button.OnPress onPress) {
		return Button.builder(label, onPress).bounds(0, 0, 60, ROW_HEIGHT).build();
	}

	private Button button(Component label, Component tooltip, Button.OnPress onPress) {
		return Button.builder(label, onPress).bounds(0, 0, 60, ROW_HEIGHT).tooltip(Tooltip.create(tooltip)).build();
	}

	// =================================================================================================== tabs

	private void buildGeneral() {
		InvScaleConfig config = config();
		this.heading(Component.translatable("invscale.heading.general"));
		this.row(Component.translatable("invscale.option.enabled"), this.toggle(config.enabled,
				Component.translatable("invscale.option.enabled.tooltip"), value -> config.enabled = value));

		CycleButton<ScaleMode> mode = CycleButton.builder(ScaleMode::label, config.scaleMode)
				.withValues(ScaleMode.values())
				.withTooltip(value -> Tooltip.create(value.description()))
				.displayOnlyValue()
				.create(0, 0, 100, ROW_HEIGHT, Component.translatable("invscale.option.mode"), (button, value) -> {
					config.scaleMode = value;
					this.changed();
				});
		this.row(Component.translatable("invscale.option.mode"), mode);
		this.row(Component.translatable("invscale.option.pixel_snap"), this.toggle(config.pixelSnap,
				Component.translatable("invscale.option.pixel_snap.tooltip"), value -> config.pixelSnap = value));
		this.row(Component.translatable("invscale.option.high_res_items"), this.toggle(config.highResItems,
				Component.translatable("invscale.option.high_res_items.tooltip"), value -> config.highResItems = value));
		this.row(Component.translatable("invscale.option.modded_screens"), this.toggle(config.scaleModdedScreens,
				Component.translatable("invscale.option.modded_screens.tooltip"), value -> config.scaleModdedScreens = value));

		this.heading(Component.translatable("invscale.heading.overview"));
		this.dynamicText(() -> Component.translatable("invscale.overview.hotbar", ConfigMath.formatScale(profile().hotbar.scale)), Theme.TEXT);
		this.dynamicText(() -> Component.translatable("invscale.overview.inventory", ConfigMath.formatScale(profile().inventoryScale)), Theme.TEXT);
		this.dynamicText(() -> Component.translatable("invscale.overview.containers", ConfigMath.formatScale(profile().containerScale)), Theme.TEXT);
		this.dynamicText(() -> Component.translatable("invscale.overview.hud", ConfigMath.formatScale(profile().hudScale)), Theme.TEXT);
		this.dynamicText(() -> Component.translatable("invscale.overview.gui_scale", ScreenScaler.guiScale()), Theme.TEXT_DIM);
		this.cursor += 4;
		this.text(Component.translatable("invscale.hint.general"), Theme.TEXT_MUTED);
	}

	private void buildHotbar() {
		HotbarSettings hotbar = profile().hotbar;
		this.scaleBlock("invscale.scale.hotbar", HotbarSettings.MIN_SCALE, HotbarSettings.MAX_SCALE,
				() -> profile().hotbar.scale, value -> profile().hotbar.scale = value,
				0.5, 0.75, 1.0, 1.5, 2.0);

		this.row(Component.translatable("invscale.option.custom_hotbar"), this.toggle(hotbar.enabled,
				Component.translatable("invscale.option.custom_hotbar.tooltip"), value -> profile().hotbar.enabled = value));
		this.heading(Component.translatable("invscale.heading.position"));
		this.row(Component.translatable("invscale.option.anchor"), this.anchorButton());
		this.row(Component.translatable("invscale.option.offset_x"), this.offsetSlider(Math.max(40, this.width / 2),
				() -> profile().hotbar.offsetX, value -> profile().hotbar.offsetX = value));
		this.row(Component.translatable("invscale.option.offset_y"), this.offsetSlider(Math.max(40, this.height),
				() -> profile().hotbar.offsetY, value -> profile().hotbar.offsetY = value));
		this.fullRow(this.button(Component.translatable("invscale.button.reset_position"), button -> {
			profile().hotbar.resetPosition();
			this.changed();
			this.rebuildPending = true;
		}));
		this.row(Component.translatable("invscale.option.bars_follow"), this.toggle(profile().barsFollowHotbar,
				Component.translatable("invscale.option.bars_follow.tooltip"), value -> profile().barsFollowHotbar = value));
		this.text(Component.translatable("invscale.hint.hotbar"), Theme.TEXT_MUTED);
	}

	private CycleButton<HotbarAnchor> anchorButton() {
		return CycleButton.builder(HotbarAnchor::label, profile().hotbar.anchor)
				.withValues(HotbarAnchor.values())
				.displayOnlyValue()
				.create(0, 0, 100, ROW_HEIGHT, Component.translatable("invscale.option.anchor"), (button, value) -> {
					profile().hotbar.anchor = value;
					this.changed();
				});
	}

	private void buildInventory() {
		this.scaleBlock("invscale.scale.inventory", ScaleProfile.SCREEN_MIN_SCALE, ScaleProfile.SCREEN_MAX_SCALE,
				() -> profile().inventoryScale, value -> profile().inventoryScale = value,
				1.0, 2.0, 3.0, 4.0);
		this.row(Component.translatable("invscale.option.scale_inventory"), this.toggle(profile().inventoryEnabled,
				value -> profile().inventoryEnabled = value));
		this.dynamicText(() -> this.effectiveText(ScreenCategory.PLAYER_INVENTORY), Theme.TEXT_DIM);

		this.heading(Component.translatable("invscale.heading.per_screen"));
		this.overrideRow(ScreenCategory.PLAYER_INVENTORY);
		this.overrideRow(ScreenCategory.CREATIVE_INVENTORY);
		this.text(Component.translatable("invscale.hint.override"), Theme.TEXT_MUTED);

		if (this.minecraft != null && this.minecraft.player != null && this.minecraft.gameMode != null
				&& !this.minecraft.gameMode.isServerControlledInventory()) {
			this.fullRow(this.button(Component.translatable("invscale.button.test_inventory"),
					Component.translatable("invscale.button.test_inventory.tooltip"), button -> {
						ConfigManager.save();
						this.minecraft.setScreen(new InventoryScreen(this.minecraft.player));
					}));
		}
	}

	private void buildContainers() {
		this.scaleBlock("invscale.scale.containers", ScaleProfile.SCREEN_MIN_SCALE, ScaleProfile.SCREEN_MAX_SCALE,
				() -> profile().containerScale, value -> profile().containerScale = value,
				1.0, 1.5, 2.0, 3.0);
		this.row(Component.translatable("invscale.option.scale_containers"), this.toggle(profile().containersEnabled,
				value -> profile().containersEnabled = value));
		this.dynamicText(() -> this.effectiveText(previewCategory), Theme.TEXT_DIM);

		this.heading(Component.translatable("invscale.heading.per_screen"));
		this.text(Component.translatable("invscale.hint.override"), Theme.TEXT_MUTED);
		for (ScreenCategory screen : ScreenCategory.values()) {
			if (!screen.isInventoryGroup()) {
				this.overrideRow(screen);
			}
		}
		this.fullRow(this.button(Component.translatable("invscale.button.clear_overrides"), button -> {
			profile().screenOverrides.entrySet().removeIf(entry -> {
				ScreenCategory screen = ScreenCategory.byKey(entry.getKey());
				return screen != null && !screen.isInventoryGroup();
			});
			this.changed();
			this.refreshAll();
		}));
	}

	private void overrideRow(ScreenCategory screen) {
		OverrideSlider slider = new OverrideSlider(0, 0, 100, ROW_HEIGHT,
				ScaleProfile.SCREEN_MIN_SCALE, ScaleProfile.SCREEN_MAX_SCALE, SCALE_STEP,
				() -> profile().override(screen),
				value -> {
					profile().setOverride(screen, value);
					previewCategory = screen;
					this.changed();
				},
				() -> screen.isInventoryGroup() ? profile().inventoryScale : profile().containerScale);
		this.row(screen.displayName(), slider);
	}

	private Component effectiveText(ScreenCategory screen) {
		PreviewRenderer.ContainerSpec spec = PreviewRenderer.spec(screen);
		ScreenScaler.Result result = ScreenScaler.compute(config(), screen, spec.width(), spec.height(), this.width, this.height);
		MutableComponent text = Component.translatable("invscale.effective", screen.displayName(),
				ConfigMath.formatScale(result.factor()),
				String.format(Locale.ROOT, "%.1f", result.factor() * ScreenScaler.guiScale()));
		if (result.limited()) {
			text.append(Component.translatable("invscale.effective.limited").withStyle(ChatFormatting.GOLD));
		}
		return text;
	}

	private void buildHud() {
		this.scaleBlock("invscale.scale.hud", ScaleProfile.HUD_MIN_SCALE, ScaleProfile.HUD_MAX_SCALE,
				() -> profile().hudScale, value -> profile().hudScale = value,
				0.75, 0.9, 1.0, 1.25);
		this.row(Component.translatable("invscale.option.hud_scaling"), this.toggle(profile().hudEnabled,
				Component.translatable("invscale.option.hud_scaling.tooltip"), value -> profile().hudEnabled = value));

		this.heading(Component.translatable("invscale.heading.elements"));
		this.text(Component.translatable("invscale.hint.elements"), Theme.TEXT_MUTED);
		int toggleWidth = Math.max(28, this.fieldWidth / 4);
		for (HudElementType type : HudElementType.values()) {
			HudElementSettings settings = profile().hud(type);
			this.labels.add(new Label(this.fit(type.displayName(), this.labelWidth - 4), this.innerX, this.cursor + 6, Theme.TEXT_DIM));

			CycleButton<Boolean> enabled = CycleButton.onOffBuilder(settings.enabled)
					.displayOnlyValue()
					.create(this.fieldX, 0, toggleWidth, ROW_HEIGHT, Component.empty(), (button, value) -> {
						profile().hud(type).enabled = value;
						this.changed();
					});
			enabled.setTooltip(Tooltip.create(Component.translatable("invscale.option.element_enabled.tooltip")));
			this.place(enabled);

			CycleButton<Boolean> visible = CycleButton.booleanBuilder(
							Component.translatable("invscale.value.shown"),
							Component.translatable("invscale.value.hidden").withStyle(ChatFormatting.RED),
							settings.visible)
					.displayOnlyValue()
					.create(this.fieldX + toggleWidth + 2, 0, toggleWidth + 6, ROW_HEIGHT, Component.empty(), (button, value) -> {
						profile().hud(type).visible = value;
						this.changed();
					});
			visible.setTooltip(Tooltip.create(Component.translatable("invscale.option.element_visible.tooltip")));
			this.place(visible);

			int sliderX = this.fieldX + toggleWidth * 2 + 10;
			ValueSlider scale = new ValueSlider(sliderX, 0, this.innerX + this.innerWidth - sliderX, ROW_HEIGHT,
					HudElementSettings.MIN_SCALE, HudElementSettings.MAX_SCALE, SCALE_STEP,
					() -> profile().hud(type).scale,
					value -> {
						profile().hud(type).scale = value;
						this.changed();
					},
					value -> Component.literal(ConfigMath.formatScale(value)));
			scale.setTooltip(Tooltip.create(Component.translatable("invscale.option.element_scale.tooltip")));
			this.place(scale);
			this.cursor += ROW_HEIGHT + ROW_GAP;
		}
	}

	private static Component targetName(int target) {
		return target == 0 ? Component.translatable("invscale.target.hotbar") : HudElementType.values()[target - 1].displayName();
	}

	private void buildPositions() {
		HudElementType[] types = HudElementType.values();
		List<Integer> targets = new ArrayList<>();
		for (int i = 0; i <= types.length; i++) {
			targets.add(i);
		}
		positionTarget = Mth.clamp(positionTarget, 0, types.length);

		this.heading(Component.translatable("invscale.heading.position"));
		CycleButton<Integer> target = CycleButton.builder(InvScaleScreen::targetName, positionTarget)
				.withValues(targets)
				.displayOnlyValue()
				.create(0, 0, 100, ROW_HEIGHT, Component.translatable("invscale.option.element"), (button, value) -> {
					positionTarget = value;
					this.rebuildPending = true;
				});
		this.row(Component.translatable("invscale.option.element"), target);

		if (positionTarget == 0) {
			this.row(Component.translatable("invscale.option.anchor"), this.anchorButton());
			this.row(Component.translatable("invscale.option.offset_x"), this.offsetSlider(Math.max(40, this.width / 2),
					() -> profile().hotbar.offsetX, value -> profile().hotbar.offsetX = value));
			this.row(Component.translatable("invscale.option.offset_y"), this.offsetSlider(Math.max(40, this.height),
					() -> profile().hotbar.offsetY, value -> profile().hotbar.offsetY = value));
		} else {
			HudElementType type = types[positionTarget - 1];
			this.row(Component.translatable("invscale.option.offset_x"), this.offsetSlider(Math.max(40, this.width),
					() -> profile().hud(type).offsetX, value -> profile().hud(type).offsetX = value));
			this.row(Component.translatable("invscale.option.offset_y"), this.offsetSlider(Math.max(40, this.height),
					() -> profile().hud(type).offsetY, value -> profile().hud(type).offsetY = value));
			if (!profile().hud(type).enabled) {
				this.text(Component.translatable("invscale.hint.element_disabled"), Theme.WARN);
			}
		}

		this.fullRow(
				this.button(Component.translatable("invscale.button.reset_position"), button -> {
					if (positionTarget == 0) {
						profile().hotbar.resetPosition();
					} else {
						profile().hud(types[positionTarget - 1]).resetPosition();
					}
					this.changed();
					this.rebuildPending = true;
				}),
				this.button(Component.translatable("invscale.button.reset_all_positions"), button -> {
					profile().resetAllPositions();
					this.changed();
					this.rebuildPending = true;
				}));
		this.text(Component.translatable("invscale.hint.positions"), Theme.TEXT_MUTED);
	}

	private void buildPresets() {
		InvScaleConfig config = config();
		List<String> names = Presets.allNames();
		if (selectedPreset == null || !names.contains(selectedPreset)) {
			selectedPreset = names.contains(config.activePreset) ? config.activePreset : Presets.DEFAULT;
		}

		this.heading(Component.translatable("invscale.heading.presets"));
		for (String name : names) {
			MutableComponent label = Component.literal(name);
			if (Presets.isBuiltIn(name)) {
				label = Component.literal("★ ").withStyle(ChatFormatting.GOLD).append(label.withStyle(ChatFormatting.WHITE));
				if (Presets.hasOverride(name)) {
					label.append(Component.literal(" *").withStyle(ChatFormatting.GRAY));
				}
			}
			if (name.equals(config.activePreset)) {
				label.append(Component.translatable("invscale.preset.active").withStyle(ChatFormatting.AQUA));
			}
			if (name.equals(selectedPreset)) {
				label = Component.literal("▶ ").withStyle(ChatFormatting.AQUA).append(label);
			}
			this.fullRow(Button.builder(label, button -> {
				selectedPreset = name;
				this.rebuildPending = true;
			}).bounds(0, 0, 100, ROW_HEIGHT).build());
		}

		this.heading(Component.translatable("invscale.heading.manage"));
		EditBox nameBox = new EditBox(this.font, 0, 0, 100, ROW_HEIGHT, Component.translatable("invscale.option.preset_name"));
		nameBox.setMaxLength(Presets.MAX_NAME_LENGTH);
		nameBox.setHint(Component.translatable("invscale.option.preset_name.hint"));
		nameBox.setValue(selectedPreset);
		this.presetNameBox = this.row(Component.translatable("invscale.option.preset_name"), nameBox);

		this.fullRow(
				this.button(Component.translatable("invscale.button.load"), Component.translatable("invscale.button.load.tooltip"), button -> {
					String name = selectedPreset;
					if (name != null && Presets.load(name)) {
						config.pvpToggleStash = null;
						ConfigManager.save();
						this.showToast(Component.translatable("invscale.message.preset_loaded", name), Theme.GOOD);
						this.rebuildPending = true;
					}
				}),
				this.button(Component.translatable("invscale.button.save"), Component.translatable("invscale.button.save.tooltip"), button -> {
					Presets.Result result = Presets.save(this.presetNameValue());
					this.handleResult(result, "invscale.message.preset_saved");
				}));
		this.fullRow(
				this.button(Component.translatable("invscale.button.rename"), Component.translatable("invscale.button.rename.tooltip"), button -> {
					String from = selectedPreset;
					if (from != null) {
						this.handleResult(Presets.rename(from, this.presetNameValue()), "invscale.message.preset_renamed");
					}
				}),
				this.button(Component.translatable("invscale.button.delete"), Component.translatable("invscale.button.delete.tooltip"), button -> {
					String name = selectedPreset;
					if (name != null) {
						Presets.Result result = Presets.delete(name);
						if (result.success() && !Presets.exists(name)) {
							selectedPreset = null;
						}
						this.handleResult(result, "invscale.message.preset_deleted");
					}
				}),
				this.button(Component.translatable("invscale.button.reset_preset"), Component.translatable("invscale.button.reset_preset.tooltip"), button -> {
					String name = selectedPreset;
					if (name != null) {
						this.handleResult(Presets.reset(name), "invscale.message.preset_reset");
					}
				}));

		this.heading(Component.translatable("invscale.heading.profiles"));
		this.text(Component.translatable("invscale.hint.profiles"), Theme.TEXT_MUTED);
		this.row(Component.translatable("invscale.option.profile_a"), this.presetPicker(config.profileA, value -> config.profileA = value));
		this.row(Component.translatable("invscale.option.profile_b"), this.presetPicker(config.profileB, value -> config.profileB = value));
	}

	private String presetNameValue() {
		return this.presetNameBox == null ? "" : this.presetNameBox.getValue();
	}

	private void handleResult(Presets.Result result, String successKey) {
		if (result.success()) {
			if (result.name() != null && Presets.exists(result.name())) {
				selectedPreset = result.name();
			}
			ConfigManager.save();
			this.showToast(Component.translatable(successKey, result.name()), Theme.GOOD);
			this.rebuildPending = true;
		} else {
			this.showToast(Component.translatable(result.errorKey()), Theme.WARN);
		}
	}

	private CycleButton<String> presetPicker(String initial, Consumer<String> setter) {
		List<String> names = Presets.allNames();
		String value = names.contains(initial) ? initial : Presets.DEFAULT;
		return CycleButton.builder((String name) -> Component.literal(name), value)
				.withValues(names)
				.displayOnlyValue()
				.create(0, 0, 100, ROW_HEIGHT, Component.empty(), (button, picked) -> {
					setter.accept(picked);
					this.changed();
				});
	}

	private void buildKeybinds() {
		this.heading(Component.translatable("invscale.heading.keybinds"));
		this.keyRow(InvScaleKeys.openSettings);
		this.keyRow(InvScaleKeys.togglePvp);
		this.keyRow(InvScaleKeys.toggleProfiles);
		this.fullRow(this.button(Component.translatable("invscale.button.open_controls"), button -> this.openControls()));
		this.text(Component.translatable("invscale.hint.keybinds"), Theme.TEXT_MUTED);

		this.heading(Component.translatable("invscale.heading.commands"));
		for (String command : new String[] {"invscale", "invscale reset", "invscale preset <name>", "invscale presets",
				"invscale save <name>", "invscale hotbar <scale>", "invscale inventory <scale>",
				"invscale containers <scale>", "invscale hud <scale>", "invscale toggle", "invscale status"}) {
			this.labels.add(new Label(Component.literal("/" + command).withStyle(ChatFormatting.AQUA).getVisualOrderText(),
					this.innerX, this.cursor, Theme.TEXT));
			this.cursor += 11;
		}
	}

	private void keyRow(KeyMapping mapping) {
		Button button = Button.builder(this.keyText(mapping), pressed -> this.openControls())
				.bounds(0, 0, 100, ROW_HEIGHT)
				.tooltip(Tooltip.create(Component.translatable("invscale.button.change_key")))
				.build();
		this.row(Component.translatable(mapping.getName()), button);
	}

	private Component keyText(KeyMapping mapping) {
		return mapping.isUnbound()
				? Component.translatable("invscale.value.unbound").withStyle(ChatFormatting.GRAY)
				: mapping.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.YELLOW);
	}

	private void openControls() {
		if (this.minecraft != null) {
			this.minecraft.setScreen(new KeyBindsScreen(this, this.minecraft.options));
		}
	}

	// =================================================================================================== actions

	private void changed() {
		ConfigManager.onChanged();
	}

	private void refreshAll() {
		for (Refreshable refreshable : this.refreshables) {
			refreshable.refresh();
		}
	}

	private Component resetLabel() {
		return Util.getMillis() < this.resetArmedUntil
				? Component.translatable("invscale.button.reset.confirm").withStyle(ChatFormatting.GOLD)
				: Component.translatable("invscale.button.reset");
	}

	private void onResetPressed() {
		if (Util.getMillis() >= this.resetArmedUntil) {
			this.resetArmedUntil = Util.getMillis() + 3000;
			return;
		}
		this.resetArmedUntil = 0;
		this.resetCategory();
		this.changed();
		this.showToast(Component.translatable("invscale.message.reset", this.category.label()), Theme.GOOD);
		this.rebuildPending = true;
	}

	/** Resets only what the current tab controls. */
	private void resetCategory() {
		InvScaleConfig config = config();
		ScaleProfile defaults = new ScaleProfile();
		ScaleProfile profile = profile();
		switch (this.category) {
			case GENERAL -> {
				InvScaleConfig fresh = new InvScaleConfig();
				config.enabled = fresh.enabled;
				config.scaleMode = fresh.scaleMode;
				config.pixelSnap = fresh.pixelSnap;
				config.highResItems = fresh.highResItems;
				config.scaleModdedScreens = fresh.scaleModdedScreens;
			}
			case HOTBAR -> {
				profile.hotbar = defaults.hotbar;
				profile.barsFollowHotbar = defaults.barsFollowHotbar;
			}
			case INVENTORY -> {
				profile.inventoryEnabled = defaults.inventoryEnabled;
				profile.inventoryScale = defaults.inventoryScale;
				profile.setOverride(ScreenCategory.PLAYER_INVENTORY, null);
				profile.setOverride(ScreenCategory.CREATIVE_INVENTORY, null);
			}
			case CONTAINERS -> {
				profile.containersEnabled = defaults.containersEnabled;
				profile.containerScale = defaults.containerScale;
				for (ScreenCategory screen : ScreenCategory.values()) {
					if (!screen.isInventoryGroup()) {
						profile.setOverride(screen, null);
					}
				}
			}
			case HUD -> {
				profile.hudEnabled = defaults.hudEnabled;
				profile.hudScale = defaults.hudScale;
				for (HudElementType type : HudElementType.values()) {
					HudElementSettings settings = profile.hud(type);
					settings.enabled = true;
					settings.visible = true;
					settings.scale = 1.0;
				}
			}
			case POSITIONS -> profile.resetAllPositions();
			case PRESETS -> {
				ConfigManager.resetToDefaults();
				selectedPreset = Presets.DEFAULT;
			}
			case KEYBINDS -> {
				InvScaleKeys.openSettings.setKey(InvScaleKeys.openSettings.getDefaultKey());
				InvScaleKeys.togglePvp.setKey(InvScaleKeys.togglePvp.getDefaultKey());
				InvScaleKeys.toggleProfiles.setKey(InvScaleKeys.toggleProfiles.getDefaultKey());
				KeyMapping.resetMapping();
				if (this.minecraft != null) {
					this.minecraft.options.save();
				}
			}
		}
	}

	private void apply() {
		ConfigManager.save();
		this.showToast(Component.translatable("invscale.message.saved"), Theme.GOOD);
	}

	private void showToast(Component message, int color) {
		this.toast = message;
		this.toastColor = color;
		this.toastUntil = Util.getMillis() + 2500;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.rebuildPending) {
			this.rebuildPending = false;
			this.rebuildWidgets();
		}
		if (this.resetButton != null) {
			this.resetButton.setMessage(this.resetLabel());
		}
	}

	@Override
	public void onClose() {
		ConfigManager.save();
		if (this.minecraft != null) {
			this.minecraft.setScreen(this.parent);
		}
	}

	@Override
	public void removed() {
		ConfigManager.saveIfDirty();
		super.removed();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// =================================================================================================== input

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		boolean handled = super.mouseClicked(event, doubleClick);
		GuiEventListener focused = this.getFocused();
		if (handled && focused instanceof AbstractSliderButton slider && this.isDragging()) {
			this.dragging = slider;
		}
		return handled;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		this.dragging = null;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		boolean overPanel = mouseX >= this.panelX && mouseX < this.panelX + this.panelWidth
				&& mouseY >= this.viewTop && mouseY < this.viewBottom;
		if (overPanel && this.contentHeight > this.viewHeight()) {
			this.setScroll(this.scroll - scrollY * 18.0);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	// =================================================================================================== rendering

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (this.minecraft == null || this.minecraft.level == null) {
			super.renderBackground(graphics, mouseX, mouseY, partialTick);
			return;
		}
		// No blur in-game: the real HUD behind this screen is the live preview.
		graphics.fill(0, 0, this.width, this.height, ARGB.multiplyAlpha(Theme.WORLD_DIM, this.fade));
	}

	private int faded(int color) {
		return ARGB.multiplyAlpha(color, this.fade);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		boolean previewing = this.dragging != null && this.category.showsHud();
		float target = previewing ? 0.18F : 1.0F;
		this.fade += (target - this.fade) * 0.3F;
		if (Math.abs(target - this.fade) < 0.01F) {
			this.fade = target;
		}
		float pulse = (float) (Math.sin(Util.getMillis() / 220.0) * 0.5 + 0.5);

		this.renderPreviews(graphics, pulse);
		this.renderChrome(graphics);
		this.renderContentLabels(graphics);

		for (GuiEventListener child : this.children()) {
			if (child instanceof AbstractWidget widget) {
				widget.setAlpha(widget == this.dragging ? 1.0F : this.fade);
			}
		}
		super.render(graphics, mouseX, mouseY, partialTick);
		this.renderScrollbar(graphics);
		this.renderToast(graphics);
	}

	private void renderChrome(GuiGraphics graphics) {
		InvScaleConfig config = config();

		// Header
		graphics.fill(0, 0, this.width, HEADER_HEIGHT, this.faded(Theme.HEADER));
		graphics.fill(0, HEADER_HEIGHT - 1, this.width, HEADER_HEIGHT, this.faded(Theme.ACCENT_SOFT));
		Component title = Component.literal("Inv").withStyle(ChatFormatting.BOLD)
				.append(Component.literal("Scale").withStyle(style -> style.withBold(true).withColor(Theme.ACCENT & 0xFFFFFF)));
		graphics.drawString(this.font, title, 10, 8, this.faded(Theme.TEXT), true);
		int titleEnd = 10 + this.font.width(title) + 8;
		if (this.width >= 380) {
			graphics.drawString(this.font, Component.translatable("invscale.subtitle"), titleEnd, 8, this.faded(Theme.TEXT_MUTED), false);
		}

		// Status chips on the right
		String presetName = config.activePreset + (ConfigManager.isModifiedFromPreset() ? "*" : "");
		Component chip = Component.translatable("invscale.chip.preset", presetName);
		int chipWidth = this.font.width(chip) + 12;
		int chipX = this.width - chipWidth - 8;
		graphics.fill(chipX, 5, chipX + chipWidth, 19, this.faded(0x40000000));
		graphics.renderOutline(chipX, 5, chipWidth, 14, this.faded(Theme.PANEL_BORDER));
		graphics.drawString(this.font, chip, chipX + 6, 8, this.faded(Theme.TEXT), false);
		if (!config.enabled) {
			Component off = Component.translatable("invscale.chip.disabled");
			int offWidth = this.font.width(off) + 12;
			int offX = chipX - offWidth - 4;
			graphics.fill(offX, 5, offX + offWidth, 19, this.faded(0xC0802020));
			graphics.drawString(this.font, off, offX + 6, 8, this.faded(Theme.TEXT), false);
		}

		// Sidebar
		graphics.fill(this.sidebarX, this.panelTop, this.sidebarX + this.sidebarWidth, this.panelBottom, this.faded(Theme.SIDEBAR));
		graphics.renderOutline(this.sidebarX, this.panelTop, this.sidebarWidth, this.panelBottom - this.panelTop, this.faded(Theme.PANEL_BORDER));
		int tabsBottom = this.panelTop + 4 + Category.values().length * (TAB_HEIGHT + 2);
		if (this.panelBottom - tabsBottom > 24) {
			graphics.drawString(this.font, Component.translatable("invscale.sidebar.gui_scale", ScreenScaler.guiScale()),
					this.sidebarX + 6, this.panelBottom - 22, this.faded(Theme.TEXT_MUTED), false);
			graphics.drawString(this.font, "v" + modVersion(), this.sidebarX + 6, this.panelBottom - 12, this.faded(Theme.TEXT_MUTED), false);
		}

		// Content panel
		graphics.fill(this.panelX, this.panelTop, this.panelX + this.panelWidth, this.panelBottom, this.faded(Theme.PANEL));
		graphics.renderOutline(this.panelX, this.panelTop, this.panelWidth, this.panelBottom - this.panelTop, this.faded(Theme.PANEL_BORDER));
		graphics.drawString(this.font, this.category.label().copy().withStyle(ChatFormatting.BOLD), this.innerX, this.panelTop + 6,
				this.faded(Theme.TEXT), false);
		FormattedCharSequence description = this.fit(this.category.description(),
				this.innerWidth - this.font.width(this.category.label()) - 14);
		graphics.drawString(this.font, description, this.innerX + this.font.width(this.category.label().copy().withStyle(ChatFormatting.BOLD)) + 8,
				this.panelTop + 6, this.faded(Theme.TEXT_MUTED), false);
		graphics.fill(this.innerX, this.panelTop + 18, this.innerX + this.innerWidth, this.panelTop + 19, this.faded(Theme.HEADER_LINE));
	}

	private void renderContentLabels(GuiGraphics graphics) {
		graphics.enableScissor(this.panelX, this.viewTop, this.panelX + this.panelWidth, this.viewBottom);
		int offset = this.viewTop - (int) this.scroll;
		for (Label label : this.labels) {
			int y = offset + label.contentY();
			if (y > this.viewTop - 10 && y < this.viewBottom) {
				graphics.drawString(this.font, label.text(), label.x(), y, this.faded(label.color()), false);
			}
		}
		for (DynamicLabel label : this.dynamicLabels) {
			int y = offset + label.contentY();
			if (y > this.viewTop - 10 && y < this.viewBottom) {
				Component text = label.text().get();
				int x = label.alignRight() ? label.x() - this.font.width(text) : label.x();
				graphics.drawString(this.font, text, x, y, this.faded(label.color()), false);
			}
		}
		graphics.disableScissor();
	}

	private void renderScrollbar(GuiGraphics graphics) {
		int viewHeight = this.viewHeight();
		if (this.contentHeight <= viewHeight) {
			return;
		}
		int trackX = this.panelX + this.panelWidth - 5;
		graphics.fill(trackX, this.viewTop, trackX + 2, this.viewBottom, this.faded(Theme.SCROLL_TRACK));
		int thumbHeight = Math.max(16, viewHeight * viewHeight / this.contentHeight);
		double maxScroll = this.contentHeight - viewHeight;
		int thumbY = this.viewTop + (int) ((viewHeight - thumbHeight) * (this.scroll / maxScroll));
		graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, this.faded(Theme.SCROLL_THUMB));
	}

	private void renderToast(GuiGraphics graphics) {
		if (this.toast == null || Util.getMillis() > this.toastUntil) {
			return;
		}
		float remaining = (this.toastUntil - Util.getMillis()) / 2500.0F;
		float alpha = Math.min(1.0F, remaining * 4.0F);
		int textWidth = this.font.width(this.toast);
		int x = (this.width - textWidth) / 2;
		int y = HEADER_HEIGHT + 2;
		if (this.width < 420) {
			y = this.height - FOOTER_HEIGHT - 16;
		}
		graphics.fill(x - 6, y, x + textWidth + 6, y + 13, ARGB.multiplyAlpha(0xE0101418, alpha));
		graphics.fill(x - 6, y + 12, x + textWidth + 6, y + 13, ARGB.multiplyAlpha(this.toastColor, alpha));
		graphics.drawString(this.font, this.toast, x, y + 2, ARGB.multiplyAlpha(this.toastColor, alpha), false);
	}

	private void renderPreviews(GuiGraphics graphics, float pulse) {
		if (this.category.showsHud()) {
			if (this.minecraft == null || this.minecraft.level == null) {
				PreviewRenderer.drawMockHud(graphics, this.width, this.height);
			}
			if (this.category == Category.HOTBAR || (this.category == Category.POSITIONS && positionTarget == 0)) {
				if (HudTransforms.hotbarActive()) {
					PreviewRenderer.outlineHotbar(graphics, this.width, this.height, pulse);
				}
			} else if (this.category == Category.POSITIONS) {
				HudElementType type = HudElementType.values()[Mth.clamp(positionTarget - 1, 0, HudElementType.values().length - 1)];
				float[] target = HudTransforms.elementTarget(type, this.width, this.height);
				int x = Math.round(target != null ? target[0] : type.anchor().refX(this.width));
				int y = Math.round(target != null ? target[1] : type.anchor().refY(this.height));
				PreviewRenderer.markAnchor(graphics, x, y, pulse);
			}
			return;
		}
		if (this.category == Category.INVENTORY || this.category == Category.CONTAINERS) {
			this.renderContainerPreview(graphics, this.category == Category.INVENTORY ? ScreenCategory.PLAYER_INVENTORY : previewCategory);
		}
	}

	private void renderContainerPreview(GuiGraphics graphics, ScreenCategory screen) {
		int right = this.width - 6;
		int areaWidth = right - this.previewLeft;
		int top = this.panelTop;
		int bottom = this.panelBottom;
		if (areaWidth < 90) {
			return;
		}
		graphics.fill(this.previewLeft, top, right, bottom, 0x60000000);
		graphics.renderOutline(this.previewLeft, top, areaWidth, bottom - top, Theme.PANEL_BORDER);
		graphics.drawString(this.font, Component.translatable("invscale.preview.title", screen.displayName()), this.previewLeft + 6, top + 6, Theme.TEXT, false);

		PreviewRenderer.ContainerSpec spec = PreviewRenderer.spec(screen);
		ScreenScaler.Result result = ScreenScaler.compute(config(), screen, spec.width(), spec.height(), this.width, this.height);
		float actual = result.factor();
		int boxTop = top + 20;
		int boxBottom = bottom - 16;
		float fit = Math.min(1.0F, Math.min((areaWidth - 12) / (spec.width() * actual), (boxBottom - boxTop) / (spec.height() * actual)));
		float drawScale = actual * fit;
		float drawWidth = spec.width() * drawScale;
		float drawHeight = spec.height() * drawScale;
		float x = this.previewLeft + (areaWidth - drawWidth) / 2.0F;
		float y = boxTop + (boxBottom - boxTop - drawHeight) / 2.0F;
		PreviewRenderer.drawContainer(graphics, spec, x, y, drawScale);

		Component caption = fit >= 0.999F
				? Component.translatable("invscale.preview.actual_size", ConfigMath.formatScale(actual))
				: Component.translatable("invscale.preview.scaled", ConfigMath.formatScale(actual), Math.round(fit * 100));
		graphics.drawString(this.font, this.fit(caption, areaWidth - 12), this.previewLeft + 6, bottom - 12, Theme.TEXT_DIM, false);
	}

	private static String modVersion() {
		return net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("invscale")
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.orElse("1.0.0");
	}
}
