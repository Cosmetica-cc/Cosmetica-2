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

import java.util.List;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.Supplier;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

/**
 * The widget for selecting a new icon.
 */
public class IconSelector extends Div {
    public IconSelector(ImageCosmetic icon, State<List<ImageCosmetic>> availableIcons) {
        this.icon = icon;
        this.availableIcons = availableIcons;
    }

    private final ImageCosmetic icon;
    private final State<List<ImageCosmetic>> availableIcons;
    private State<@Nullable SelectableIcon> selected; // lazy load

    @Override
    public List<Component> build() {
        List<ImageCosmetic> iconOptions = this.availableIcons.acquire(this);

        SelectableIcon[] icons = iconOptions.stream()
                .map(SelectableIcon::new)
                .toArray(SelectableIcon[]::new);

        // load selected state
        SelectableIcon initialSelect = null;
        for (SelectableIcon icon : icons)
            if (icon.cosmetic.getId().equals(this.icon.getId())) {
                initialSelect = icon;
                break;
            }
        this.selected = new State<>(initialSelect);

        return ImmutableList.of(
                new IconHeader(this.selected::acquire).tag("horizontal", "header"),
                new EntryList.Grid(icons, this.selected::acquire).tag("flex-1", "icon-selector")
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

            Text displayIcon = noIcon ? Text.translatable("label.icons.no_icon") :
                    Text.translatable("label.icons.icon", icon.cosmetic.getName());

            return ImmutableList.of(
                    new Label(displayIcon).tag("flex-1"),
                    noIcon ? new Div().tag("icon-image", "icon-replacement") : new Image(new ResourceKey(location)).tag("icon-image")
            );
        }
    }

    private class SelectableIcon extends Image {
        public SelectableIcon(ImageCosmetic cosmetic) {
            super(new ResourceKey(cosmetic.getImage().location));
            this.cosmetic = cosmetic;
            this.setTransparent(1);
        }

        private final ImageCosmetic cosmetic;

        @Override
        public void mouseClicked(Element target, double x, double y, int button) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
                if (this != IconSelector.this.selected.peek()) {
                    Logging.getInstance().debug("Setting icon " + cosmetic);
                    IconSelector.this.selected.set(this);
                }
            }
        }

        @Override
        public void render(Canvas canvas, Region region, Margins padding, int mouseX, int mouseY) {
            // hover effect
            if (region.contains(mouseX, mouseY) && !this.getStyle().get(BORDER).isPresent()) {
                canvas.drawRect(region, 0x707070);
            }
            super.render(canvas, region, padding, mouseX, mouseY);
        }
    }
}
