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

package cc.cosmetica.cosmetica.gui;

import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.cosmetica.gui.widget.EntryList;
import cc.cosmetica.kupe.api.*;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.CommonProperties;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Margins;
import cc.cosmetica.kupe.api.maths.Region;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

/**
 * Outfit selection for the anvil.
 */
public class OutfitSelectAnvilScreen extends Component implements AnimatedTextureScreen {
    public OutfitSelectAnvilScreen(NametaggableAnvil anvil) {
        this.title = ID.translationKey("screens");
        this.anvil = anvil;

        // TODO should we refresh outfits manually in case websocket is down
    }

    private final Text title;
    private final NametaggableAnvil anvil;

    @Override
    public List<Component> build() {
        List<OutfitWheelScreen.OutfitOption> options = Cosmetica.OWN_OUTFITS.acquire(this);

        SelectableOutfitA[] components = options.stream()
                .map(SelectableOutfitA::new)
                .toArray(SelectableOutfitA[]::new);

        return Arrays.asList(
                new Div(
                        new Label(this.title),
                        new Label(Text.translatable("screens.cosmetica.outfit_select_anvil.description"))
                ).tag("title"),
                new Div(
                        new EntryList.Grid(
                                components,
                                c -> null
                        ).withStyle(Style.create()
                                .set(MARGINS, fixed(new Margins(3, 0, 0, 0)))
                                .set(WIDTH, screen(75, 0))
                                .set(MIN_WIDTH, screen(75, 0))
                                .set(MIN_HEIGHT, screen(0, 60))
                                .set(EntryList.Grid.COLUMN_GAP, 2)
                                .set(EntryList.Grid.ROW_GAP, 2)
                                .set(BACKGROUND_COLOUR, OptionalInt.empty())),
                        new Button(Text.GUI_CANCEL, Screens::closeCurrentScreen)
                ).tag("body")
        );
    }

    @Override
    public @NotNull Stylesheet getStylesheet() {
        return new Stylesheet()
                .tag("body", Screen.BODY_DEFAULT_STYLE)
                .tag("title", Screen.TITLE_DEFAULT_STYLE)
                .tag("body", Style.create()
                        // 15(title margin) + 6(related to text height) + 2(extra gap)
                        .set(MARGINS, fixed(new Margins(15 + 6 + 2, 0, 0, 0))))
                .component(SelectableOutfitA.class, Style.create()
                        .set(WIDTH, fixed(OptionalInt.of(69 * 2/3)))
                        .set(HEIGHT, fixed(OptionalInt.of(69))));
    }

    public static final ResourceKey ID = new ResourceKey("cosmetica", "outfit_select_anvil");
    public static final ResourceKey NEW_OUTFIT_ICON = new ResourceKey("cosmetica", "textures/new_outfit.png");

    private static SelectableOutfitA find(SelectableOutfitA[] components, String id) {
        if (id.isEmpty()) return null;

        for (SelectableOutfitA outfit : components) {
            if (outfit.option.id.equals(id)) {
                return outfit;
            }
        }

        // none matched
        return null;
    }

    /**
     * A selectable outfit item in the menu.
     */
    class SelectableOutfitA extends LayeredSpace {
        SelectableOutfitA(OutfitWheelScreen.OutfitOption option) {
            super(true);
            this.option = option;
        }

        private final OutfitWheelScreen.OutfitOption option;

        @Override
        public List<Component> build() {
            return Arrays.asList(
                    new Image(new ResourceKey(option.thumbnail.location))
                            .crop(0, 0.1667f, 0, 0.1667f)
                            .setTransparent(option.usable ? 1.0f : 0.5f)
            );
        }

        @Override
        public void mouseClicked(Element target, double x, double y, int button) {
            if (!this.option.usable) return;
            // play click sound
            GuiUtils.playClick();
            // Set ID and close this screen
            OutfitSelectAnvilScreen.this.anvil.cosmetica$setNewName(this.option.id.replaceAll("-", ""));
            Screens.closeCurrentScreen();
        }

        @Override
        public void render(Canvas canvas, Region region, Margins padding, int mouseX, int mouseY) {
            super.render(canvas, region, padding, mouseX, mouseY);

            // hover
            if (region.contains(mouseX, mouseY)) {
                canvas.setTransparency(0.5f);
                canvas.drawRect(region, 0x77FFFFFF);
                canvas.disableTransparency();
            }
        }

        @Override
        public Stylesheet getStylesheet() {
            return new Stylesheet().self(
                    Style.create()
                            .set(TOOLTIP, Optional.of(new Tooltip(Text.literal(this.option.name))))
            );
        }

        public void paintDecorations(Canvas canvas, Region region, Region scissorRegion, int mouseX, int mouseY) {
            // Draw tooltip
            Optional<Tooltip> tooltip = this.getStyle().get(CommonProperties.TOOLTIP);

            if (tooltip.isPresent() && region.intersect(scissorRegion).contains(mouseX, mouseY)) {
                tooltip.get().render(canvas, mouseX, mouseY);
            }
        }
    }

    public interface NametaggableAnvil {
        void cosmetica$setNewName(String name);
    }
}
