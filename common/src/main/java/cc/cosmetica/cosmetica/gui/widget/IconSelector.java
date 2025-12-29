/*
 * Copyright 2024, 2025 Cosmetica
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.cosmetica.cosmetica.gui.widget;

import cc.cosmetica.core.api.CachedImage;
import cc.cosmetica.core.api.ImageCosmetic;
import cc.cosmetica.core.api.NametagConfig;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.cosmetica.util.CosmeticaLogCategory;
import cc.cosmetica.kupe.api.Canvas;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Margins;
import cc.cosmetica.kupe.api.maths.Region;
import com.google.common.collect.ImmutableList;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

/**
 * The widget for selecting a new icon.
 */
public class IconSelector extends Div {
    public IconSelector(AtomicBoolean iconDirty, State<List<IconOption>> availableIcons) {
        this.iconDirty = iconDirty;
        this.availableIcons = availableIcons;
    }

    private final AtomicBoolean iconDirty;
    private final State<List<IconOption>> availableIcons;

    @Override
    public List<Component> build() {
        List<IconOption> iconOptions = new ArrayList<>(this.availableIcons.acquire(this));

        // yes it won't refresh if selected icon is changed by another modpack, but that's fine I think
        boolean managed = Cosmetica.SELECTED_ICON.extract(this, ic -> ic.isManaged());

        // add modpack icon
        if (managed) {
            ImageCosmetic icon = Cosmetica.SELECTED_ICON.peek();
            if (iconOptions.stream().noneMatch(option -> icon.getId().equals(option.cosmetic.getId()))) {
                iconOptions.add(0, new IconOption(icon, false));
            }
        }

        SelectableIcon[] icons = iconOptions.stream()
                .map(icon -> new SelectableIcon(icon, managed))
                .toArray(SelectableIcon[]::new);

        // load selected state

        Function<Component, SelectableIcon> selectedState = t -> Cosmetica.SELECTED_ICON.extract(t, cosmetic -> {
            SelectableIcon selected = null;
            for (SelectableIcon icon : icons)
                if (icon.cosmetic.getId().equals(cosmetic.getId())) {
                    selected = icon;
                    break;
                }
            return selected;
        });

        return ImmutableList.of(
                new IconHeader(selectedState).tag("horizontal", "header"),
                new EntryList.Grid(icons, selectedState).tag("flex-1", "icon-selector")
        );
    }

    @Override
    public Stylesheet getStylesheet() {
        return new Stylesheet()
                .self(Style.create()
                        .set(MARGINS, fixed(new Margins(30, 10, 12, 10)))
                        .set(ALIGN_ITEMS, Align.STRETCH_START))
                .tag("header", Style.create()
                        .set(MARGINS, fixed(new Margins(0,0,2,0))))
                .tag("icon-image", Style.create()
                        .set(WIDTH, fixedSize(20))
                        .set(HEIGHT, fixedSize(20)))
                .tag("icon-replacement", Style.create()
                        .set(BACKGROUND_COLOUR, OptionalInt.of(0))
                        .set(BORDER, Border.create(1, 0xFFFFFF)))
                .tag("icon-selector", Style.create()
                        .set(Grid.ROW_GAP, 2)
                        .set(Grid.COLUMN_GAP, 2))
                .component(SelectableIcon.class, Style.create()
                        .set(PADDING, fixed(new Margins(1)))
                        .set(WIDTH, fixedSize(30))
                        .set(HEIGHT, fixedSize(30)));
    }

    private static class IconHeader extends Div {
        public IconHeader(Function<Component, @Nullable SelectableIcon> icon) {
            this.icon = icon;
        }

        private final Function<Component, @Nullable SelectableIcon> icon;

        @Override
        public List<Component> build() {
            @Nullable SelectableIcon icon = this.icon.apply(this);

            boolean noIcon = icon == null;
            ResourceLocation location = noIcon ? null : icon.cosmetic.getImage().location;

            Text displayIcon = noIcon ? Text.translatable("label.icons.noIcon") :
                    Text.translatable("label.icons.icon", icon.cosmetic.getName());

            return ImmutableList.of(
                    new Label(displayIcon).tag("flex-1"),
                    noIcon ? new Div().tag("icon-image", "icon-replacement") : new Image(new ResourceKey(location)).tag("icon-image")
            );
        }
    }

    private class SelectableIcon extends Image {
        public SelectableIcon(IconOption option, boolean managed) {
            super(new ResourceKey(option.cosmetic.getImage().location));
            this.cosmetic = option.cosmetic;
            this.disabled = !option.unlocked || managed;
            this.tooltip = managed ? Text.translatable("tooltip.cosmetica.icon.managed")
                    : !option.unlocked ? Text.translatable("tooltip.cosmetica.icon.notUnlocked") : null;
            this.setTransparent(!option.unlocked ? 0.8f : 1);
        }

        private final ImageCosmetic cosmetic;
        private final boolean disabled;
        private final @Nullable Text tooltip;

        @Override
        public void mouseClicked(Element target, double x, double y, int button) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_1 && !this.disabled) {
                if (this.cosmetic != Cosmetica.SELECTED_ICON.peek()) {
                    Logging.getInstance().debug(CosmeticaLogCategory.GUI, "Setting icon " + cosmetic);

                    IconSelector.this.iconDirty.set(true);
                    Cosmetica.SELECTED_ICON.set(this.cosmetic);
                }
            }
        }

        @Override
        public Stylesheet getStylesheet() {
            if (this.tooltip != null) {
                return new Stylesheet()
                        .self(Style.create().set(TOOLTIP, Optional.of(new Tooltip(this.tooltip))));
            } else {
                return null;
            }
        }

        @Override
        public void render(Canvas canvas, Region region, Margins padding, int mouseX, int mouseY) {
            // hover effect
            if (region.contains(mouseX, mouseY) && !this.getStyle().get(BORDER).isPresent() && !this.disabled) {
                canvas.drawRect(region, 0x707070);
            }
            super.render(canvas, region, padding, mouseX, mouseY);
        }
    }

    public static class IconOption {
        public IconOption(ImageCosmetic cosmetic, boolean unlocked) {
            this.cosmetic = cosmetic;
            this.unlocked = unlocked;
        }

        private final ImageCosmetic cosmetic;
        private final boolean unlocked;
    }
}
