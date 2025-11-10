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
import cc.cosmetica.core.api.OutfitCosmetics;
import cc.cosmetica.core.impl.Logging;
import cc.cosmetica.cosmetica.Cosmetica;
import cc.cosmetica.cosmetica.gui.widget.EntryList;
import cc.cosmetica.cosmetica.gui.widget.OutfitCount;
import cc.cosmetica.kupe.api.ResourceKey;
import cc.cosmetica.kupe.api.Screens;
import cc.cosmetica.kupe.api.State;
import cc.cosmetica.kupe.api.Text;
import cc.cosmetica.kupe.api.gui.*;
import cc.cosmetica.kupe.api.gui.style.Style;
import cc.cosmetica.kupe.api.gui.style.Stylesheet;
import cc.cosmetica.kupe.api.maths.Axis2D;
import cc.cosmetica.kupe.api.maths.Margins;
import gg.cloaks.javaclient.api.PremiumApi;
import gg.cloaks.javaclient.model.CopyOutfitDto;
import gg.cloaks.javaclient.model.PlanRestrictions;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static cc.cosmetica.cosmetica.Cosmetica.mainThreadExcept;
import static cc.cosmetica.kupe.api.gui.style.CommonProperties.*;

/**
 * A confirmation screen for stealing someone's look when you don't already have the outfit.
 * Prompts the user which outfit slot to use, or whether to add a new item. Shows how many outfit slots
 * are used.
 * Unregistered. Pass ID as a second parameter when setting Kupe screen.
 */
public class ReplaceOutfitSlotScreen extends Component implements AnimatedTextureScreen {
    protected ReplaceOutfitSlotScreen(Cosmetics initialCosmetics) {
        if (!initialCosmetics.getOutfitId().isPresent())
            throw new IllegalArgumentException("No outfit id for steal-their-look cosmetics?!");

        this.title = STEAL_THEIR_LOOK.translationKey("screens");
        this.newOutfit = UUID.fromString(initialCosmetics.getOutfitId().get());
        this.newOutfitCosmetics = new State<>(initialCosmetics);
        this.outfitLimit = new State<>(-1);

        CosmeticaAPI.premiumApi().requestAsync(PremiumApi::getRestrictions)
                .thenApply(PlanRestrictions::getMaxOutfits)
                .thenApply(BigDecimal::intValue)
                .thenAcceptAsync(this.outfitLimit::set, Minecraft.getInstance());

        Cosmetica.fetchOutfits();

        CosmeticaAPI.subscribe(CosmeticaAPI.SubscriptionEvent.OUTFIT, this.newOutfit, STEAL_THEIR_LOOK.toResourceLocation(), () -> {
            CosmeticaAPI.outfits().requestAsync(api -> api.get(this.newOutfit.toString()))
                    .thenApply(OutfitCosmetics::new)
                    .thenAcceptAsync(this.newOutfitCosmetics::set, Minecraft.getInstance())
                    .exceptionally(err->{
                        Logging.getInstance().error("Error updating outfit cosmetics", err);
                        return null;
                    });
        });
    }

    private final Text title;
    private final UUID newOutfit;
    private final State<Cosmetics> newOutfitCosmetics;
    private final State<Integer> outfitLimit;
    private final State<Boolean> setting = new State<>(false);
    private final State<@Nullable ReplaceableOutfit> replacing = new State<>(null);

    @Override
    public List<Component> build() {
        Cosmetics newOutfit = this.newOutfitCosmetics.acquire(this);
        boolean setting = this.setting.acquire(this);
        @Nullable ReplaceableOutfit replacing = this.replacing.acquire(this);

        // don't auto update list once button is clicked, we'll auto close soon anyway.
        // if auto-update is kept the user sees the removal before screen closes - a bit messy!
        List<OutfitWheelScreen.OutfitOption> options = setting ? Cosmetica.OWN_OUTFITS.peek() : Cosmetica.OWN_OUTFITS.acquire(this);

        List<Component> components = options.stream()
                .map(ReplaceableOutfit::new)
                .map(o -> o.tag("outfit"))
                .collect(Collectors.toCollection(ArrayList::new));
        // prepend 'new outfit'
        int outfitLimit = this.outfitLimit.acquire(this);
        int currentCount = options.size();
        if (currentCount < outfitLimit)
            components.add(0, new ReplaceableOutfit());

        final UUID player = Minecraft.getInstance().getUser().getGameProfile().getId();

        return Arrays.asList(
                new Div(
                        new Label(this.title),
                        new OutfitCount(this.outfitLimit)
                ).tag("title"),
                new Div(
                        new Div(new GUIPlayer(player, true)
                                .withStyle(Style.create()
                                        .set(MIN_WIDTH, screen(12, 0))
                                ),
                                new Label(Text.literal(newOutfit.getOutfitName().orElse("(No name)")))
                        ).tag("width-50%")
                                .withStyle(Style.create().set(PADDING, screen(6, 0, (w,h)->new Margins(0,w,0,0)))),
                        new EntryList.Grid(components.toArray(new Component[0]), k->{
                            for (Component o : components) {
                                OutfitWheelScreen.OutfitOption op = ((ReplaceableOutfit)o).option;
                                if (replacing == null && op == null) {
                                    return o;
                                } else if (replacing != null && op != null && op.id.equals(replacing.option.id)) {
                                    return o;
                                }
                            }

                            return null;
                        }).tag("width-50%").withStyle(Style.create().set(HEIGHT, screen(0, 75)))
                ).tag("body"),
                new Div(
                        new Button(Text.translatable("button.cosmetica.confirm"), () -> {
                            final ReplaceableOutfit oldOutfit = replacing;
                            if (oldOutfit != null && oldOutfit.usable) // sanity check
                            {
                                this.setting.set(true);
                                CopyOutfitDto dto = new CopyOutfitDto();
                                dto.equip(true);

                                if (oldOutfit.option == null) {
                                    CosmeticaAPI.outfits().requestAsync(api -> api.copy(this.newOutfit.toString(), dto))
                                            .thenAcceptAsync(outfit1 -> Minecraft.getInstance().setScreen(null), Minecraft.getInstance())
                                            .exceptionally(mainThreadExcept(err -> {
                                                Logging.getInstance().error("Error stealing look (new)", err);
                                                this.setting.set(false);
                                            }));
                                } else {
                                    CosmeticaAPI.outfits().requestAsync(api -> {api.delete(oldOutfit.option.id); return api;})
                                            .thenApply(api -> api.copy(this.newOutfit.toString(), dto))
                                            .thenAcceptAsync(outfit1 -> Minecraft.getInstance().setScreen(null), Minecraft.getInstance())
                                            .exceptionally(mainThreadExcept(err -> {
                                                Logging.getInstance().error("Error stealing look (replace)", err);
                                                this.setting.set(false);
                                            }));
                                }
                            }
                        }).setDisabled(replacing==null || setting),
                        new Button(Text.GUI_CANCEL, Screens::closeCurrentScreen)
                ).tag("bottom-bar")
        );
    }

    @Override
    public void unmount() {
        CosmeticaAPI.unsubscribe(CosmeticaAPI.SubscriptionEvent.OUTFIT, this.newOutfit, STEAL_THEIR_LOOK.toResourceLocation());
    }

    @Override
    public @NotNull Stylesheet getStylesheet() {
        return new Stylesheet()
                .tag("body", BODY_DEFAULT_STYLE)
                .tag("title", TITLE_DEFAULT_STYLE)
                .tag("bottom-bar", Style.create()
                        .set(MARGINS, (vw, vh, pw, ph) -> new Margins(vh - 50, 0, 0, 0))
                )
                .tag("width-50%", Style.create()
                        .set(WIDTH, screen(50, 0)))
                .component(ReplaceableOutfit.class, Style.create()
                        .set(WIDTH, fixedSize(50))
                        .set(HEIGHT, fixedSize(50)));
    }

    public static final ResourceKey STEAL_THEIR_LOOK = new ResourceKey("cosmetica", "steal_their_look");

    // Stolen & modified from kupe Screen
    private static final Style BODY_DEFAULT_STYLE = Style.create()
                    .set(WIDTH, SCREEN_WIDTH)
                    .set(HEIGHT, SCREEN_HEIGHT)
                    .set(Div.FLOW_DIRECTION, Axis2D.POSITIVE_X)//modified
                    .set(Div.JUSTIFY_CONTENT, Justify.CENTRE)
                    .set(Div.ALIGN_ITEMS, Align.CENTRE);

    private static final Style TITLE_DEFAULT_STYLE = Style.create()
                    .set(WIDTH, SCREEN_WIDTH)
                    .set(Label.ALIGN_TEXT, Align.CENTRE)
                    .set(MARGINS, fixed(new Margins(15, 0, 0, 0)));

    /**
     * A selectable outfit item in the menu.
     */
    private class ReplaceableOutfit extends Image {
        ReplaceableOutfit(OutfitWheelScreen.OutfitOption option) {
            super(new ResourceKey(option.thumbnail.location));
            this.usable = option.usable;
            this.option = option;
            this.setTransparent(option.usable ? 1.0f : 0.5f);
        }
        // "New Outfit" option
        ReplaceableOutfit() {
            super(OutfitSelectScreen.NEW_OUTFIT_ICON);
            this.option = null;
            this.usable = true;
        }

        private final boolean usable;

        final OutfitWheelScreen.OutfitOption option;

        @Override
        public void mouseClicked(Element target, double x, double y, int button) {
            if (!this.usable) return;
            // play click sound
            GuiUtils.playClick();
            ReplaceOutfitSlotScreen.this.replacing.set(this);
        }
    }
}
