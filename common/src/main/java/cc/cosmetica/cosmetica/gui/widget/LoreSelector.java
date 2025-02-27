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
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Dimensions;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

public class LoreSelector extends Div {
    public LoreSelector(NametagConfig lore) {
        this.lore = lore.getPrefix(); // core places lore text in prefix field of NametagConfig
        // TODO icons
    }

    private String lore;

    @Override
    public List<Component> build() {
        return ImmutableList.of(
                new Div(
                        new Label(Text.translatable("label.lore.lore", this.lore)).tag("flex-1"),
                        new IconButton(new ResourceKey("cosmetica", "textures/colour.png"), () -> {})
                ).tag("horizontal"),
                new EntryList(

                ).tag("flex-1"),
                new Div(
                        new Button(Text.translatable("button.lore.titles"), () -> {}).tag("lore-type"),
                        new Button(Text.translatable("button.lore.pronouns"), () -> {}).tag("lore-type"),
                        new Button(Text.translatable("button.lore.connections"), () -> {}).tag("lore-type")
                ).tag("horizontal", "lore-types")
        );
    }

    @Override
    public @Nullable Stylesheet getStylesheet() {
        return new Stylesheet()
                .self(Style.create()
                        .set(ALIGN_ITEMS, Align.STRETCH_START))
                .tag("lore-type", Style.create()
                        .set(WIDTH, percent(30, 0))
                        .set(MINIMUM_SIZE, (vw, vh, pw, ph) -> Optional.of(new Dimensions(
                                (int)Math.min(pw/3f, 62), 0
                        ))))
                .tag("lore-types", Style.create()
                        .set(JUSTIFY_CONTENT, Justify.SPACE_BETWEEN));
    }
}
