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

import cc.cosmetica.core.api.CosmeticaAPI;
import cc.cosmetica.core.api.Cosmetics;
import cc.cosmetica.core.api.NametagConfig;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.cosmetica.Setting;
import cc.cosmetica.cosmetica.gui.widget.IconButton;
import cc.cosmetica.cosmetica.gui.widget.MenuEndSelection;
import cc.cosmetica.cosmetica.gui.widget.OutfitPlayer;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.Screen;
import cc.cosmetica.kupe.api.Screens;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

/**
 * Base for home-screen-like screens. Screens that are too different (snipe) shouldn't extend this.
 */
public abstract class AbstractHomeScreen extends Screen implements AnimatedTextureScreen {
    protected AbstractHomeScreen(ResourceKey id) {
        super(id);
    }

    @Override
    protected Component[] buildScreen() {
        UUID self = Minecraft.getInstance().getUser().getGameProfile().getId();

        Cosmetics cosmetics = Cosmetica.OWN_COSMETICS.acquire(this);
        boolean authenticated = CosmeticaAPI.isAuthenticated();

        return new Component[] {
                new Div(
                        new LayeredSpace(true,
                                this.createOutfitPlayer(self, authenticated, cosmetics),
                                new Div(
                                        new IconButton(
                                                new ResourceKey("cosmetica", "textures/gear.png"),
                                                () -> Screens.setScreen(new CosmeticaSettingsScreen(CosmeticaSettingsScreen.SETTINGS_SCREEN, Setting.SETTINGS), CosmeticaSettingsScreen.SETTINGS_SCREEN)),
                                        new IconButton(
                                                new ResourceKey("minecraft", "textures/item/name_tag.png"),
                                                () -> Screens.setScreen(StyleNametagScreen.ID))
                                                .setDisabled(!authenticated)
                                                .withStyle(Cosmetica.authTooltip(authenticated))
                                ).withStyle(Style.create()
                                        .set(Div.ALIGN_ITEMS, Align.START)
                                        .set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X))
                        ).tag("main-section"),
                        this.createRightMenu(cosmetics, authenticated).tag("main-section")
                ).tag("main-content"),
                createMenuEndSelection()
        };
    }

    protected Component createMenuEndSelection() {
        return new MenuEndSelection();
    }

    protected Component createOutfitPlayer(UUID self, boolean authenticated, Cosmetics cosmetics) {
        return new OutfitPlayer(self,
                authenticated,
                Optional.ofNullable(cosmetics).flatMap(Cosmetics::getOutfitName).orElse("§7No Outfit"),
                Optional.ofNullable(cosmetics).flatMap(Cosmetics::getLore).orElse(NametagConfig.EMPTY),
                Optional.ofNullable(cosmetics).map(Cosmetics::getNametag).orElse(NametagConfig.EMPTY));
    }

    /**
     * Create the right hand menu.
     * @apiNote main-section tag is automatically applied.
     */
    @NotNull
    abstract protected Component createRightMenu(Cosmetics cosmetics, boolean authenticated);

    @Override
    public @NotNull Stylesheet getStylesheet() {
        return super.getStylesheet()
                .tag("main-content", Style.create()
                        .set(FLEX, 1)
                        .set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X)
                        .set(Div.JUSTIFY_CONTENT, Justify.CENTRE)
                        .set(Div.ALIGN_ITEMS, Align.CENTRE))
                .tag("main-section", Style.create()
                        .set(WIDTH, screen(50, 0))
                        .set(HEIGHT, percent(0, 100)));
    }
}
