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

import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.Screens;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Margins;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

/**
 * A div containing the done and web panel buttons for the menu.
 */
public class MenuEndSelection extends Div {
    private final State<Boolean> CLICKED = new State<>(false);

    @Override
    public List<Component> build() {
        boolean clicked = CLICKED.acquire(this);

        return ImmutableList.of(
                new Button(Text.GUI_DONE, Screens::closeCurrentScreen),
                new IconButton(new ResourceKey("cosmetica", "textures/internet.png"), () -> {
                    Cosmetica.openWebPanel();
                    CLICKED.set(true);
                })
                .withStyle(Style.create()
                        .set(TOOLTIP, Optional.of(new Tooltip(
                                clicked ? Text.translatable("tooltip.cosmetica.copiedURL")
                                        : Text.translatable("tooltip.cosmetica.openWebPanel")
                        )))
                )
        );
    }

    @Override
    public @Nullable Stylesheet getStylesheet() {
        return new Stylesheet()
                .self(Style.create()
                        .set(MARGINS, fixed(new Margins(0, 0, 12, 0)))
                );
    }
}
