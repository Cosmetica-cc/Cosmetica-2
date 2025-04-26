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
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.cosmetica.Setting;
import cc.cosmetica.cosmetica.StateHolder;
import cc.cosmetica.cosmetica.gui.widget.*;
import cc.cosmetica.kupe.api.*;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import cc.cosmetica.kupe.api.maths.Margins;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

/**
 * Shows the cosmetics of an inspected cosmetics holder.
 * Unregistered. Pass ID as second parameter when setting kupe screen.
 */
public class SnipeScreen extends Screen {
    public static final ResourceKey ID = new ResourceKey("cosmetica", "snipe");

    public SnipeScreen(LivingEntity entity) {
        super(entity instanceof Player ?
                Text.literal(entity.getDisplayName().getString()) :
                Text.literal(Cosmetics.getCosmetics(entity).flatMap(Cosmetics::getOutfitName).orElse("Outfit"))
        );

        // TODO armour stands dont have an autoupdated cosmetic state currently. we subscribe to automatic outfit updates, so this should be done?
        this.cosmetics = ((StateHolder)entity).cosmetica$getCosmeticState();
        this.playerUUID = entity instanceof Player ? entity.getUUID() : null;
    }

    private final State<Cosmetics> cosmetics;
    private final @Nullable UUID playerUUID;

    @Override
    protected Component[] buildScreen() {
        Cosmetics outfit = this.cosmetics.acquire(this);
        UUID player = playerUUID == null ? Minecraft.getInstance().getUser().getGameProfile().getId() : playerUUID;

        // we can do something similar to home screen.
        List<CosmeticEntry> entryList = new ArrayList<>();
        CosmeticaHomeScreen.populateEntryList(entryList, outfit);

        return new Component[] {
                new Div(
                        new Div(
                                new Div().withStyle(Style.create().set(HEIGHT, fixedSize(10))),
                                new FakePlayer(player, true)
                                    .withStyle(Style.create().set(WIDTH, screen(12, 0)))
                        ).tag("main-section")
                                .withStyle(Style.create()
                                        .set(Div.JUSTIFY_CONTENT, Justify.CENTRE)),
                        new CosmeticsBrowser(entryList)
                                .tag("main-section")
                ).tag("main-content"),
                new Button(Text.translatable("button.cosmetica.stealHisLook"), Screens::closeCurrentScreen),
                new Button(Text.GUI_DONE, Screens::closeCurrentScreen)
        };
    }

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
                        .set(HEIGHT, percent(0, 100)))
                .tag("body", Style.create()
                        .set(PADDING, fixed(new Margins(0, 0, 10, 0))));
    }
}
