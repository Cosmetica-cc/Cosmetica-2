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
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.Screen;
import cc.cosmetica.kupe.api.Screens;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.Button;
import cc.cosmetica.kupe.api.gui.Component;
import cc.cosmetica.kupe.api.gui.Element;
import cc.cosmetica.kupe.api.gui.Image;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Dimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

/**
 * The menu outfit select screen. For the wheel, see {@link OutfitWheelScreen}.
 */
public class OutfitSelectScreen extends Screen {
    public OutfitSelectScreen() {
        super(ID);
    }

    @Override
    protected Component[] buildScreen() {
        List<OutfitWheelScreen.OutfitOption> options = Cosmetica.OWN_OUTFITS.acquire(this);

        SelectableOutfit[] components = options.stream()
                .map(SelectableOutfit::new)
                .toArray(SelectableOutfit[]::new);

        return new Component[] {
                new EntryList.Grid(
                        components,
                        grid -> Cosmetica.SELECTED_OUTFIT_ID.extract(grid, id -> find(components, id.orElse("")))
                ).withStyle(Style.create()
                        .set(WIDTH, screen(75, 0))
                        .set(MIN_WIDTH, screen(75, 0))
                        .set(MIN_HEIGHT, screen(0, 75))
                        .set(EntryList.Grid.COLUMN_GAP, 2)
                        .set(EntryList.Grid.ROW_GAP, 2)
                        .set(BACKGROUND_COLOUR, OptionalInt.empty())),
                new Button(Text.GUI_DONE, Screens::closeCurrentScreen)
        };
    }

    @Override
    public @NotNull Stylesheet getStylesheet() {
        return super.getStylesheet()
                .component(SelectableOutfit.class, Style.create()
                        .set(WIDTH, fixed(OptionalInt.of(50)))
                        .set(HEIGHT, fixed(OptionalInt.of(50))));
    }

    private static class SelectableOutfit extends Image {
        SelectableOutfit(OutfitWheelScreen.OutfitOption option) {
            super(new ResourceKey(option.thumbnail.location));
            this.option = option;
            this.setTransparent(option.usable ? 1.0f : 0.5f);
        }

        private final OutfitWheelScreen.OutfitOption option;

        @Override
        public void mouseClicked(Element target, double x, double y, int button) {
            if (!this.option.usable) return;
            if (this.option.id.equals(Cosmetica.SELECTED_OUTFIT_ID.peek().orElse(""))) return;
            // play click sound
            GuiUtils.playClick();
            // visually set & make request
            this.option.equipAsync();
        }
    }

    private static SelectableOutfit find(SelectableOutfit[] components, String id) {
        if (id.isEmpty()) return null;

        for (SelectableOutfit outfit : components) {
            if (outfit.option.id.equals(id)) {
                return outfit;
            }
        }

        // none matched
        return null;
    }

    public static final ResourceKey ID = new ResourceKey("cosmetica", "outfit_select");
}
