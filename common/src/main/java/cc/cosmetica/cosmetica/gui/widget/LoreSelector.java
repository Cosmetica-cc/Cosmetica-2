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

import cc.cosmetica.core.api.NametagConfig;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.kupe.api.Canvas;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Dimensions;
import cc.cosmetica.kupe.api.maths.Margins;
import cc.cosmetica.kupe.api.maths.Region;
import com.google.common.collect.ImmutableList;
import gg.cloaks.javaclient.model.LoreOptions;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

public class LoreSelector extends Div {
    public LoreSelector(NametagConfig lore, State<LoreOptions> availableLores) {
        this.lore = lore.getPrefix(); // core places lore text in prefix field of NametagConfig
        this.availableLores = availableLores;
    }

    private String lore;
    private final State<LoreOptions> availableLores;
    private State<Integer> lorePage = new State<>(0);
    private State<@Nullable SelectableLore> selected; // lazy load

    @Override
    public List<Component> build() {
        LoreOptions loreOptions = this.availableLores.acquire(this);
        int page = lorePage.acquire(this);

        SelectableLore[] loreValues;
        switch (page) {
            case 0:
                loreValues = loreOptions.getTitles().stream()
                        .map(SelectableLore::new)
                        .toArray(SelectableLore[]::new);
                break;
            case 1:
                loreValues = loreOptions.getPronouns().stream()
                        .map(SelectableLore::new)
                        .toArray(SelectableLore[]::new);
                break;
            case 2:
            default:
                loreValues = new SelectableLore[0];
                break;
        }

        // load selected state
        SelectableLore initialSelect = null;
        for (SelectableLore lore : loreValues)
            if (lore.lore.equals(this.lore)) {
                initialSelect = lore;
                break;
            }
        this.selected = new State<>(initialSelect);

        return ImmutableList.of(
                new LoreHeader(this.selected::acquire).tag("horizontal", "header"),
                page == 2 ? new Div(
                        new Div().tag("flex-1"),
                        new Label(Text.translatable("label.lore.referToWebsite")),
                        new Button(Text.translatable("button.lore.openWebPanel"), Cosmetica::openWebPanel),
                        new Div().withStyle(Style.create().set(FLEX, 3))
                ).tag("flex-1", "refer-to-website")
                : new EntryList.Div(loreValues, this.selected)
                        .selected(
                                Style.create()
                                        .set(BACKGROUND_COLOUR, OptionalInt.of(0xFFFFFF))
                                        .set(Label.TEXT_COLOUR, 0x333333)
                        ).tag("flex-1"),
                new Div(
                        new Button(Text.translatable("button.lore.titles"), () -> {
                            this.lorePage.set(0);
                        }).setDisabled(page == 0).tag("lore-type"),
                        new Button(Text.translatable("button.lore.pronouns"), () -> {
                            this.lorePage.set(1);
                        }).setDisabled(page == 1).tag("lore-type"),
                        new Button(Text.translatable("button.lore.connections"), () -> {
                            this.lorePage.set(2);
                        }).setDisabled(page == 2).tag("lore-type")
                ).tag("horizontal", "lore-types")
        );
    }

    @Override
    public @Nullable Stylesheet getStylesheet() {
        return new Stylesheet()
                .self(Style.create()
                        .set(MARGINS, fixed(new Margins(30, 10, 12, 10)))
                        .set(ALIGN_ITEMS, Align.STRETCH_START))
                .tag("header", Style.create()
                        .set(MARGINS, fixed(new Margins(0,0,2,0))))
                .tag("refer-to-website", Style.create()
                        .set(BACKGROUND_COLOUR, OptionalInt.of(0))
                        .set(PADDING, fixed(new Margins(2)))
                        .set(Label.ALIGN_TEXT, Align.CENTRE))
                .tag("lore-type", Style.create()
                        .set(WIDTH, percent(33, 0)))
                .tag("lore-types", Style.create()
                        .set(JUSTIFY_CONTENT, Justify.SPACE_BETWEEN))
                .component(SelectableLore.class, Style.create()
                        .set(PADDING, fixed(new Margins(1))));
    }

    private static class LoreHeader extends Div {
        public LoreHeader(Function<Component, @Nullable SelectableLore> icon) {
            this.icon = icon;
        }

        private final Function<Component, @Nullable SelectableLore> icon;

        @Override
        public List<Component> build() {
            @Nullable SelectableLore lore = this.icon.apply(this);

            Text displayLore = lore == null ? Text.translatable("label.lore.no_lore") : Text.translatable("label.lore.lore", lore.lore);

            return ImmutableList.of(
                    new Label(displayLore).tag("flex-1"),
//                        new IconButton(new ResourceKey("cosmetica", "textures/colour.png"), () -> {}),
                    new IconButton(new ResourceKey("cosmetica", "textures/remove.png"), () -> {})
            );
        }
    }

    private class SelectableLore extends Label {
        public SelectableLore(String lore) {
            super(Text.literal(lore));
            this.lore = lore;
        }

        private final String lore;

        @Override
        public void mouseClicked(double x, double y, int button) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
                if (this != LoreSelector.this.selected.peek()) {
                    LoreSelector.this.selected.set(this);
                }
            }
        }

        @Override
        public void render(Canvas canvas, Region region, Margins padding, int mouseX, int mouseY) {
            // hover effect
            if (region.shrinkMargins(new Margins(0,6,0,0)).contains(mouseX, mouseY) && !this.getStyle().get(BORDER).isPresent()) {
                canvas.drawRect(region.addMargins(padding), 0x707070);
            }
            super.render(canvas, region, padding, mouseX, mouseY);
        }
    }
}
