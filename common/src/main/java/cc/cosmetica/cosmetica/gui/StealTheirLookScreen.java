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
import cc.cosmetica.kupe.api.*;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import cc.cosmetica.kupe.api.maths.Margins;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

/**
 * A confirmation screen for stealing someone's look.
 * Unregistered. Pass ID as a second parameter when setting Kupe screen.
 */
public class StealTheirLookScreen extends Screen {
    protected StealTheirLookScreen(State<Cosmetics> cosmeticsState) {
        super(STEAL_THEIR_LOOK);
        this.cosmetics = cosmeticsState;
    }

    private final State<Cosmetics> cosmetics;

    @Override
    protected Component[] buildScreen() {
        Cosmetics outfit = this.cosmetics.acquire(this);
        final UUID player = Minecraft.getInstance().getUser().getGameProfile().getId();

        return new Component[] {
                new LayeredSpace(true,
                        new Div(
                                new FakePlayer(player, true)
                                        .withStyle(Style.create().set(WIDTH, screen(12, 0)))
                        ).withStyle(Style.create()
                                .set(Div.JUSTIFY_CONTENT, Justify.CENTRE)),
                        new Div(
                                new Button(Text.translatable("button.cosmetica.confirm"), () -> {
                                    // TODO set outfit
                                    //todo ability to set previous screen for kupe
                                    Screens.closeCurrentScreen();
                                    Screens.closeCurrentScreen();
                                    Screens.setScreen(CosmeticaHomeScreen.ID);
                                    }),
                                new Button(Text.GUI_CANCEL, Screens::closeCurrentScreen)
                        ).tag("bottom-bar")
                ).withStyle(Style.create().set(FLEX, 1)),

        };
    }

    @Override
    public @NotNull Stylesheet getStylesheet() {
        return super.getStylesheet()
                .component(Button.class, Style.create()
                        .set(WIDTH, fixedSize(150)))
                .tag("bottom-bar", Style.create()
                        .set(MARGINS, (vw, vh, pw, ph) -> new Margins(ph - 80, 0, 0, 0))
                        .set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X));
    }

    public static final ResourceKey STEAL_THEIR_LOOK = new ResourceKey("cosmetica", "steal_their_look");
}
