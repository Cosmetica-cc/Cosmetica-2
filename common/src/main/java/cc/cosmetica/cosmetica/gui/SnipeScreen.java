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
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.cosmetica.StateHolder;
import cc.cosmetica.cosmetica.gui.widget.*;
import cc.cosmetica.kupe.api.*;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import cc.cosmetica.kupe.api.maths.Margins;
import gg.cloaks.javaclient.api.DefaultApi;
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
        this.isSetting = new State<>(false);
    }

    private final State<Cosmetics> cosmetics;
    private final State<Boolean> isSetting;
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
                        new CosmeticsBrowser(entryList, false)
                                .tag("main-section")
                ).tag("main-content"),
                new StealTheirLookButton(
                        outfit, this.isSetting,
                        Text.translatable("button.cosmetica.stealHisLook"),
                        () -> {
                            this.isSetting.set(true);
                            String outfitId = outfit.getOutfitId().orElse("");
                            if (outfitId.isEmpty()) {
                                CosmeticaAPI.performAsync(DefaultApi::outfitsControllerUnequip)
                                        .thenAccept(__ -> {
                                            Logging.getInstance().debug("Cleared Cosmetics by Steal-their-look.");
                                            Minecraft.getInstance().tell(Screens::closeCurrentScreen);
                                        })
                                        .exceptionally(err -> {
                                            Logging.getInstance().error("Failed to unequip cosmetics!", err);
                                            Minecraft.getInstance().tell(()->this.isSetting.set(false));
                                            return null;
                                        });
                                return;
                            }

                            // test stealtheirlookscreen: never take the quick option
                            // if not empty : either own cosmetics (e.g. armour stand) or not own cosmetics (need to select a slot)
                            if (false &&Cosmetica.OWN_OUTFITS.peek().stream().anyMatch(option -> option.id.equals(outfitId))) {
                                // can set cosmetics immediately
                                CosmeticaAPI.performAsync(api->api.outfitsControllerEquip(outfitId))
                                        .thenAccept(__ -> {
                                            Logging.getInstance().debug("Set Cosmetics by Steal-their-look.");
                                            Minecraft.getInstance().tell(Screens::closeCurrentScreen);
                                        })
                                        .exceptionally(err -> {
                                            Logging.getInstance().error("Failed to set cosmetics!", err);
                                            Minecraft.getInstance().tell(()->this.isSetting.set(false));
                                            return null;
                                        });
                            } else {
                                Screens.setScreen(new ReplaceOutfitSlotScreen(outfit), ReplaceOutfitSlotScreen.STEAL_THEIR_LOOK);
                            }
                        }),
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
                        .set(HEIGHT, (vw,vh,pw,ph) -> OptionalInt.of(ph*50/100 + 100)))
                .tag("body", Style.create()
                        .set(PADDING, fixed(new Margins(0, 0, 10, 0))));
    }

    /**
     * Reloads when your own cosmetics change (prevent having to reload whole screen). This might be overkill optimisation.
     */
    private static class StealTheirLookButton extends Button {
        public StealTheirLookButton(Cosmetics outfit, State<Boolean> disabled, Text text, Runnable onClicked) {
            super(text, onClicked);
            this.outfit = outfit;
            this.disabledState = disabled;
        }
        private final Cosmetics outfit;
        private final State<Boolean> disabledState;

        @Override
        public List<Component> build() {
            @Nullable Cosmetics cosmetics1 = Cosmetica.OWN_COSMETICS.acquire(this);
            boolean overrideDisabled = this.disabledState.acquire(this);

            boolean disabled = overrideDisabled || (cosmetics1 != null && cosmetics1.getOutfitId().equals(outfit.getOutfitId()));
            this.setDisabled(disabled);
            this.withStyle(Style.create().set(TOOLTIP, disabled ? Optional.of(new Tooltip(Text.translatable("tooltip.cosmetica.outfitAlreadySelected"))) : Optional.empty()));
            return super.build();
        }
    }
}
