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

import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.api.NametagConfig;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.cosmetica.gui.widget.IconSelector;
import cc.cosmetica.cosmetica.gui.widget.LoreSelector;
import cc.cosmetica.cosmetica.gui.widget.MenuEndSelection;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.Screen;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import cc.cosmetica.kupe.api.maths.Margins;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

public class StyleNametagScreen extends Screen {
    public StyleNametagScreen() {
        super(ID);
    }

    @Override
    protected Component[] buildScreen() {
        UUID self = Minecraft.getInstance().getUser().getGameProfile().getId();
        Cosmetics cosmetics = Cosmetica.OWN_COSMETICS.acquire(this);

        return new Component[] {
                new Div(
                        new LoreSelector(cosmetics == null ? NametagConfig.EMPTY : cosmetics.getLore().orElse(NametagConfig.EMPTY))
                                .tag("flex-1"),
                        new FakePlayer(self, true),
                        new IconSelector(cosmetics == null ? NametagConfig.EMPTY : cosmetics.getNametag())
                                .tag("flex-1")
                ).tag("horizontal", "flex-1", "main-content"),
                new MenuEndSelection()
        };
    }

    @Override
    public @Nullable Stylesheet getStylesheet() {
        return super.getStylesheet()
                .component(FakePlayer.class, Style.create()
                        .set(WIDTH, fixed(OptionalInt.of(50)))
                        .set(ALIGN_SELF, Optional.of(Align.CENTRE)))
                .tag("horizontal", Style.create()
                        .set(WIDTH, percent(100, 0))
                        .set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X))
                .tag("main-content", Style.create()
                        .set(Div.JUSTIFY_CONTENT, Justify.SPACE_AROUND)
                        .set(Div.ALIGN_ITEMS, Align.STRETCH_CENTRE))
                .tag("flex-1", Style.create()
                        .set(FLEX, 1));
    }

    public static final ResourceKey ID = new ResourceKey("cosmetica", "name_tag");
}
