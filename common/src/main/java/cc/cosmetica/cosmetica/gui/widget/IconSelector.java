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
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.OptionalInt;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

public class IconSelector extends Div {
    public IconSelector(NametagConfig config) {
        this.config = config;
    }

    private final NametagConfig config;

    @Override
    public List<Component> build() {
        return ImmutableList.of(
                new Div(
                        new Label(Text.translatable("label.icons.icon", this.config.getIcon().getName())).tag("flex-1"),
                        new Image(new ResourceKey(this.config.getIcon().getImage().location)).tag("icon-image")
                ).tag("horizontal"),
                new EntryList( // todo tile grid instead

                ).tag("flex-1"),
                new Div(
                ).tag("buffer-20-height") // Lore selector has buttons at bottom. Yes we could use margins.
        );
    }

    @Override
    public @Nullable Stylesheet getStylesheet() {
        return new Stylesheet()
                .self(Style.create()
                        .set(ALIGN_ITEMS, Align.STRETCH_START))
                .tag("icon-image", Style.create()
                        .set(WIDTH, fixed(OptionalInt.of(20)))
                        .set(HEIGHT, fixed(OptionalInt.of(20))))
                .tag("buffer-20-height", Style.create()
                        .set(HEIGHT, fixed(OptionalInt.of(20))));
    }
}
